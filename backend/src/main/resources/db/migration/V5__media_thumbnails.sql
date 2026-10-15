-- Downscaled JPEG used by lists and grids; NULL for assets uploaded before thumbnails existed (served as the original).
ALTER TABLE media_assets ADD COLUMN thumbnail_key VARCHAR(255) NULL;
