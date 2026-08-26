param([string]$ApiBase = 'http://localhost:8080/api/v1')

$ErrorActionPreference = 'Stop'
$rootUri = $ApiBase -replace '/api/v1$', ''
$suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds().ToString()
$ownerName = 'owner_' + $suffix.Substring($suffix.Length - 8)
$memberName = 'member_' + $suffix.Substring($suffix.Length - 8)

function Invoke-Api {
    param([string]$Method, [string]$Path, [string]$Token = '', $Body = $null)
    $headers = @{}
    if ($Token) { $headers.Authorization = 'Bearer ' + $Token }
    $params = @{ Uri = $ApiBase + $Path; Method = $Method; Headers = $headers }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json'
        $params.Body = $Body | ConvertTo-Json -Depth 8 -Compress
    }
    try { Invoke-RestMethod @params }
    catch { throw "$Method $Path failed: $($_.ErrorDetails.Message)" }
}

$health = Invoke-RestMethod ($rootUri + '/actuator/health')
if ($health.status -ne 'UP') { throw 'Health check failed' }
$unauthorizedHasRequestId = $false
try { Invoke-RestMethod ($ApiBase + '/activities') | Out-Null }
catch {
    $errorEnvelope = $_.ErrorDetails.Message | ConvertFrom-Json
    $unauthorizedHasRequestId = $errorEnvelope.code -eq 'UNAUTHORIZED' -and -not [string]::IsNullOrWhiteSpace($errorEnvelope.requestId)
}
if (-not $unauthorizedHasRequestId) { throw 'Unauthorized response has no request ID' }

$registration = @{ password = 'Password123!'; nickname = 'Owner'; username = $ownerName }
Invoke-Api POST '/auth/register' '' $registration | Out-Null
$registration.username = $memberName
$registration.nickname = 'Member'
Invoke-Api POST '/auth/register' '' $registration | Out-Null

$ownerLogin = Invoke-Api POST '/auth/login' '' @{ username = $ownerName; password = 'Password123!' }
$memberLogin = Invoke-Api POST '/auth/login' '' @{ username = $memberName; password = 'Password123!' }
$ownerToken = $ownerLogin.data.accessToken
$memberToken = $memberLogin.data.accessToken
$memberId = $memberLogin.data.user.id
$now = Get-Date

$created = Invoke-Api POST '/activities' $ownerToken @{
    title = 'HTTP full-flow smoke activity'; description = 'Automated smoke test'; location = 'Test venue'
    signupStart = $now.AddMinutes(-1).ToString('s'); signupEnd = $now.AddDays(1).ToString('s')
    startTime = $now.AddDays(2).ToString('s'); endTime = $now.AddDays(3).ToString('s')
    visibility = 'INVITE_ONLY'; feeRule = 'AA'
}
$activityId = $created.data.id
$invite = $created.data.invitationCode
if (-not $activityId -or $invite.Length -ne 10) { throw 'Activity creation failed' }
$imagePath = Resolve-Path (Join-Path $PSScriptRoot '..\frontend\src\static\logo.png')
$coverResponse = Invoke-RestMethod -Uri ($ApiBase + '/activities/' + $activityId + '/cover') -Method Post `
    -Headers @{ Authorization = 'Bearer ' + $ownerToken } -Form @{ file = Get-Item $imagePath }
if (-not $coverResponse.data.coverUrl) { throw 'Cover upload failed' }

$discover = Invoke-Api GET '/activities?scope=discover&size=50' $memberToken
if ($discover.data.items.id -contains $activityId) { throw 'Private activity leaked into discover list' }
$preview = Invoke-Api GET ('/activities/invitations/' + $invite) $memberToken
if ($preview.data.id -ne $activityId) { throw 'Invitation preview mismatch' }
Invoke-Api GET ('/activities/' + $activityId + '?invitationCode=' + $invite) $memberToken | Out-Null
Invoke-Api POST ('/activities/' + $activityId + '/signups') $memberToken @{
    invitationCode = $invite; grade = 'Grade 2'; passengerCount = 1; remark = 'Smoke member'
} | Out-Null

$message = Invoke-Api POST ('/activities/' + $activityId + '/messages') $memberToken @{ content = 'Smoke chat message' }
if ($message.data.content -ne 'Smoke chat message') { throw 'Chat send failed' }
$messages = Invoke-Api GET ('/activities/' + $activityId + '/messages?afterId=0&limit=20') $ownerToken
if ($messages.data.Count -lt 1) { throw 'Chat history failed' }

$photoResponse = Invoke-RestMethod -Uri ($ApiBase + '/activities/' + $activityId + '/photos') -Method Post `
    -Headers @{ Authorization = 'Bearer ' + $memberToken } -Form @{ file = Get-Item $imagePath }
if (-not $photoResponse.data.url) { throw 'Photo upload failed' }
$photos = Invoke-Api GET ('/activities/' + $activityId + '/photos') $ownerToken
if ($photos.data.Count -ne 1) { throw 'Photo list is missing or duplicated' }

Invoke-Api PUT ('/activities/' + $activityId + '/locations/me') $memberToken @{
    latitude = 31.2304; longitude = 121.4737; address = 'Smoke location'
} | Out-Null
$locations = Invoke-Api GET ('/activities/' + $activityId + '/locations') $ownerToken
if ($locations.data.Count -ne 1) { throw 'Location sharing failed' }
Invoke-Api DELETE ('/activities/' + $activityId + '/locations/me') $memberToken | Out-Null
$locationsAfterStop = Invoke-Api GET ('/activities/' + $activityId + '/locations') $ownerToken
if ($locationsAfterStop.data.Count -ne 0) { throw 'Location stop failed' }

$task = Invoke-Api POST ('/activities/' + $activityId + '/sign-tasks') $ownerToken @{
    title = 'Gathering check-in'; description = 'HTTP smoke'
}
$taskId = $task.data.id
$record = Invoke-Api POST ('/sign-tasks/' + $taskId + '/records') $memberToken @{
    latitude = 31.2304; longitude = 121.4737; address = 'Meeting point'; remark = 'Arrived'
}
if (-not $record.data.id) { throw 'Attendance record failed' }
$summary = Invoke-Api GET ('/sign-tasks/' + $taskId + '/summary') $ownerToken
$signedCount = @($summary.data.members | Where-Object signed).Count
if ($signedCount -lt 1) { throw 'Attendance summary failed' }
$photoTask = Invoke-Api POST ('/activities/' + $activityId + '/sign-tasks') $ownerToken @{
    title = 'Photo check-in'; description = 'Multipart smoke'
}
$photoRecord = Invoke-RestMethod -Uri ($ApiBase + '/sign-tasks/' + $photoTask.data.id + '/records') -Method Post `
    -Headers @{ Authorization = 'Bearer ' + $memberToken } -Form @{
        latitude = '31.2304'; longitude = '121.4737'; address = 'Photo point'; remark = 'Photo proof'; file = Get-Item $imagePath
    }
if (-not $photoRecord.data.photoUrl) { throw 'Multipart attendance photo failed' }
$history = Invoke-Api GET '/users/me/sign-records' $memberToken
if ($history.data.Count -lt 2) { throw 'Attendance history failed' }

$vehicle = Invoke-Api POST ('/activities/' + $activityId + '/vehicles') $ownerToken @{
    plateNumber = 'TEST-SMOKE'; driverName = 'Smoke driver'
    pickupTime = $now.AddDays(2).AddHours(-1).ToString('s'); pickupLocation = 'Main entrance'
}
if (-not $vehicle.data.id) { throw 'Vehicle creation failed' }
$vehicles = Invoke-Api GET ('/activities/' + $activityId + '/vehicles') $memberToken
if ($vehicles.data.Count -lt 1) { throw 'Vehicle list failed' }

Invoke-Api POST ('/activities/' + $activityId + '/transfer') $ownerToken @{ newCreatorId = $memberId } | Out-Null
$oldCreatorForbidden = $false
try { Invoke-Api GET ('/activities/' + $activityId + '/signups') $ownerToken | Out-Null }
catch { $oldCreatorForbidden = $_.Exception.Message -match 'FORBIDDEN' }
if (-not $oldCreatorForbidden) { throw 'Creator permission was not transferred immediately' }
$members = Invoke-Api GET ('/activities/' + $activityId + '/signups') $memberToken
if ($members.data.Count -ne 2) { throw 'New creator cannot read members' }

$profile = Invoke-Api PATCH '/users/me' $memberToken @{ nickname = 'New leader' }
if ($profile.data.nickname -ne 'New leader') { throw 'Profile update failed' }
$rotated = Invoke-Api POST '/auth/refresh' '' @{ refreshToken = $memberLogin.data.refreshToken }
if (-not $rotated.data.accessToken) { throw 'Token refresh failed' }
Invoke-Api POST '/auth/logout' $rotated.data.accessToken @{ refreshToken = $rotated.data.refreshToken } | Out-Null

[pscustomobject]@{
    health = $health.status; activityId = $activityId; invitationCodeLength = $invite.Length
    members = $members.data.Count; photos = $photos.data.Count; locationsAfterStop = $locationsAfterStop.data.Count
    signed = $signedCount; signPhoto = [bool]$photoRecord.data.photoUrl; vehicles = $vehicles.data.Count
    creatorTransferred = $oldCreatorForbidden; requestIdOnUnauthorized = $unauthorizedHasRequestId; refreshAndLogout = $true
} | ConvertTo-Json -Compress
