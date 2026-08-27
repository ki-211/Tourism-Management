Add-Type -AssemblyName System.Drawing

$iconDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\frontend\src\static\icons'))

function New-RoundedRectPath([float]$x, [float]$y, [float]$width, [float]$height, [float]$radius) {
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $diameter = $radius * 2
    $path.AddArc($x, $y, $diameter, $diameter, 180, 90)
    $path.AddArc($x + $width - $diameter, $y, $diameter, $diameter, 270, 90)
    $path.AddArc($x + $width - $diameter, $y + $height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($x, $y + $height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function New-TabIcon([string]$name, [string]$colorHex, [scriptblock]$draw) {
    $bitmap = [System.Drawing.Bitmap]::new(64, 64, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $color = [System.Drawing.ColorTranslator]::FromHtml($colorHex)
    $pen = [System.Drawing.Pen]::new($color, 4.2)
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
    & $draw $graphics $pen $color
    $target = Join-Path $iconDir $name
    $bitmap.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
    $pen.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}

$discover = {
    param($g, $pen, $color)
    $g.DrawEllipse($pen, 9, 9, 46, 46)
    $points = [System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new(39, 20),
        [System.Drawing.PointF]::new(34, 35),
        [System.Drawing.PointF]::new(20, 43),
        [System.Drawing.PointF]::new(27, 28)
    )
    $brush = [System.Drawing.SolidBrush]::new($color)
    $g.FillPolygon($brush, $points)
    $brush.Dispose()
}

$checkin = {
    param($g, $pen, $color)
    $path = New-RoundedRectPath 10 10 44 44 12
    $g.DrawPath($pen, $path)
    $g.DrawLines($pen, [System.Drawing.PointF[]]@(
        [System.Drawing.PointF]::new(20, 32),
        [System.Drawing.PointF]::new(28, 40),
        [System.Drawing.PointF]::new(44, 23)
    ))
    $path.Dispose()
}

$calendar = {
    param($g, $pen, $color)
    $path = New-RoundedRectPath 9 14 46 40 9
    $g.DrawPath($pen, $path)
    $g.DrawLine($pen, 10, 26, 54, 26)
    $g.DrawLine($pen, 21, 9, 21, 19)
    $g.DrawLine($pen, 43, 9, 43, 19)
    $brush = [System.Drawing.SolidBrush]::new($color)
    $g.FillEllipse($brush, 19, 34, 5, 5)
    $g.FillEllipse($brush, 30, 34, 5, 5)
    $g.FillEllipse($brush, 41, 34, 5, 5)
    $g.FillEllipse($brush, 19, 44, 5, 5)
    $g.FillEllipse($brush, 30, 44, 5, 5)
    $brush.Dispose()
    $path.Dispose()
}

$user = {
    param($g, $pen, $color)
    $g.DrawEllipse($pen, 22, 9, 20, 20)
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddBezier(13, 53, 14, 39, 21, 34, 32, 34)
    $path.AddBezier(32, 34, 43, 34, 50, 39, 51, 53)
    $g.DrawPath($pen, $path)
    $path.Dispose()
}

$marker = {
    param($g, $pen, $color)
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $path.AddBezier(32, 57, 27, 49, 13, 36, 13, 24)
    $path.AddBezier(13, 24, 13, 11, 21, 5, 32, 5)
    $path.AddBezier(32, 5, 43, 5, 51, 11, 51, 24)
    $path.AddBezier(51, 24, 51, 36, 37, 49, 32, 57)
    $path.CloseFigure()
    $brush = [System.Drawing.SolidBrush]::new($color)
    $g.FillPath($brush, $path)
    $inner = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
    $g.FillEllipse($inner, 24, 16, 16, 16)
    $inner.Dispose()
    $brush.Dispose()
    $path.Dispose()
}

$icons = @(
    @{ Base = 'discover-v2'; Legacy = 'list'; Draw = $discover },
    @{ Base = 'checkin-v2'; Legacy = 'sign'; Draw = $checkin },
    @{ Base = 'journey-v2'; Legacy = 'myActivity'; Draw = $calendar },
    @{ Base = 'profile-v2'; Legacy = 'user'; Draw = $user }
)

foreach ($icon in $icons) {
    New-TabIcon ($icon.Base + '.png') '#7A8986' $icon.Draw
    New-TabIcon ($icon.Base + '-active.png') '#0F766E' $icon.Draw
    New-TabIcon ($icon.Legacy + '.png') '#7A8986' $icon.Draw
    New-TabIcon ($icon.Legacy + '-active.png') '#0F766E' $icon.Draw
}

New-TabIcon 'marker-self.png' '#0F766E' $marker
New-TabIcon 'marker-member.png' '#E76F51' $marker
