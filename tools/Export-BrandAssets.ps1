$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$project = Split-Path $PSScriptRoot -Parent
$source = Join-Path $project 'design/logo-proposals/round-2/02-fenix-outdoor.png'
$image = [System.Drawing.Image]::FromFile($source)
try {
    # Mechanical size exports only: preserve the selected artwork and its framing.
    $outputs = @(
        @{ Path = 'app/src/main/res/drawable-nodpi/brand_logo.png'; Size = 512 },
        @{ Path = 'app/src/main/res/drawable-nodpi/app_icon_legacy.png'; Size = 192 },
        @{ Path = 'garmin/resources/launcher-v08.png'; Size = 65 },
        @{ Path = 'garmin/resources/brand-v08.png'; Size = 48 },
        @{ Path = 'garmin/resources/shutter-v09.png'; Size = 256 }
    )
    foreach ($output in $outputs) {
        $destination = Join-Path $project $output.Path
        New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
        $bitmap = New-Object System.Drawing.Bitmap($output.Size, $output.Size)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
            try {
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
                $graphics.DrawImage($image, 0, 0, $output.Size, $output.Size)
            } finally { $graphics.Dispose() }
            $bitmap.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
        } finally { $bitmap.Dispose() }
        Write-Output ($output.Path + ' (' + $output.Size + ' px)')
    }
} finally { $image.Dispose() }
