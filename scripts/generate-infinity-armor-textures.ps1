Set-StrictMode -Version Latest
Add-Type -AssemblyName System.Drawing

$projectRoot = Split-Path -Parent $PSScriptRoot
$textureRoot = Join-Path $projectRoot 'src/main/resources/assets/armoredarsenal/textures'

$transparent = [System.Drawing.Color]::Transparent
$void = [System.Drawing.ColorTranslator]::FromHtml('#0B0918')
$obsidian = [System.Drawing.ColorTranslator]::FromHtml('#17132B')
$shadow = [System.Drawing.ColorTranslator]::FromHtml('#24174B')
$violet = [System.Drawing.ColorTranslator]::FromHtml('#4A2A91')
$brightViolet = [System.Drawing.ColorTranslator]::FromHtml('#7553D7')
$cyan = [System.Drawing.ColorTranslator]::FromHtml('#22DDF4')
$ice = [System.Drawing.ColorTranslator]::FromHtml('#C7FBFF')
$star = [System.Drawing.ColorTranslator]::FromHtml('#FFFFFF')

function Fill-Rect($image, $color, $x, $y, $width, $height) {
    $graphics = [System.Drawing.Graphics]::FromImage($image)
    $brush = [System.Drawing.SolidBrush]::new($color)
    $graphics.FillRectangle($brush, $x, $y, $width, $height)
    $brush.Dispose()
    $graphics.Dispose()
}

function Set-Pixel($image, $color, $x, $y) {
    $image.SetPixel($x, $y, $color)
}

function New-Texture($relativePath, $width, $height, [scriptblock]$draw) {
    $path = Join-Path $textureRoot $relativePath
    $directory = Split-Path -Parent $path
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $image = [System.Drawing.Bitmap]::new($width, $height)
    & $draw $image
    $image.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $image.Dispose()
}

New-Texture 'entity/equipment/humanoid/infinity/outer.png' 64 32 {
    param($image)

    foreach ($rect in @(
        @(8, 0, 8, 8), @(16, 0, 8, 8), @(0, 8, 32, 8),
        @(20, 16, 16, 4), @(16, 20, 24, 12),
        @(44, 16, 8, 4), @(40, 20, 16, 12),
        @(4, 16, 8, 4), @(0, 20, 16, 12)
    )) {
        Fill-Rect $image $obsidian $rect[0] $rect[1] $rect[2] $rect[3]
    }

    Fill-Rect $image $shadow 8 8 8 8
    Fill-Rect $image $violet 8 8 2 8
    Fill-Rect $image $violet 14 8 2 8
    Fill-Rect $image $cyan 9 10 6 1
    Set-Pixel $image $ice 11 10
    Set-Pixel $image $ice 12 10
    Fill-Rect $image $cyan 11 11 2 4
    Set-Pixel $image $brightViolet 10 8
    Set-Pixel $image $brightViolet 13 8

    Fill-Rect $image $shadow 20 20 8 12
    Fill-Rect $image $violet 20 20 2 12
    Fill-Rect $image $violet 26 20 2 12
    Set-Pixel $image $cyan 20 21
    Set-Pixel $image $cyan 27 21
    Set-Pixel $image $cyan 21 22
    Set-Pixel $image $cyan 26 22
    Fill-Rect $image $cyan 22 23 4 1
    Fill-Rect $image $cyan 23 24 2 6
    Fill-Rect $image $ice 23 23 2 1
    Set-Pixel $image $violet 22 30
    Set-Pixel $image $violet 25 30

    Fill-Rect $image $shadow 44 20 4 12
    Fill-Rect $image $violet 44 20 1 12
    Fill-Rect $image $cyan 45 22 1 8
    Fill-Rect $image $brightViolet 46 20 2 2
    Fill-Rect $image $violet 48 20 4 12
    Fill-Rect $image $cyan 50 23 1 7
    Set-Pixel $image $ice 50 24

    Fill-Rect $image $shadow 4 20 4 12
    Fill-Rect $image $violet 4 20 1 12
    Fill-Rect $image $cyan 5 22 1 8
    Fill-Rect $image $brightViolet 6 20 2 2
    Fill-Rect $image $violet 8 20 4 12
    Fill-Rect $image $cyan 10 23 1 7

    foreach ($point in @(@(3,10), @(19,4), @(23,12), @(29,10), @(21,26), @(26,28), @(45,25), @(53,21), @(6,27), @(14,23))) {
        Set-Pixel $image $star $point[0] $point[1]
    }
}

New-Texture 'entity/equipment/humanoid_leggings/infinity/inner.png' 64 32 {
    param($image)

    foreach ($rect in @(
        @(20, 16, 16, 4), @(16, 20, 24, 12),
        @(4, 16, 8, 4), @(0, 20, 16, 12),
        @(20, 0, 8, 4), @(16, 4, 16, 12)
    )) {
        Fill-Rect $image $obsidian $rect[0] $rect[1] $rect[2] $rect[3]
    }

    Fill-Rect $image $shadow 20 20 8 12
    Fill-Rect $image $brightViolet 20 20 2 4
    Fill-Rect $image $brightViolet 26 20 2 4
    Fill-Rect $image $cyan 23 20 2 11
    Set-Pixel $image $ice 23 22
    Set-Pixel $image $ice 24 22
    Fill-Rect $image $shadow 4 20 4 12
    Fill-Rect $image $violet 4 20 1 12
    Fill-Rect $image $cyan 6 22 1 9
    Fill-Rect $image $violet 8 20 4 12
    Fill-Rect $image $cyan 9 22 1 9
    Set-Pixel $image $star 21 27
    Set-Pixel $image $star 26 25
    Set-Pixel $image $star 7 25
    Set-Pixel $image $star 10 29
}

New-Texture 'item/infinity_helmet.png' 16 16 {
    param($image)
    Fill-Rect $image $brightViolet 3 2 2 3
    Fill-Rect $image $brightViolet 11 2 2 3
    Fill-Rect $image $violet 2 5 12 7
    Fill-Rect $image $obsidian 3 6 10 7
    Fill-Rect $image $cyan 4 7 8 1
    Fill-Rect $image $cyan 7 8 2 4
    Set-Pixel $image $ice 7 7
    Set-Pixel $image $ice 8 7
    Set-Pixel $image $star 4 10
    Set-Pixel $image $star 11 9
}

New-Texture 'item/infinity_chestplate.png' 16 16 {
    param($image)
    Fill-Rect $image $violet 2 3 4 3
    Fill-Rect $image $brightViolet 10 2 4 4
    Fill-Rect $image $obsidian 4 4 8 9
    Fill-Rect $image $shadow 5 5 6 8
    Set-Pixel $image $cyan 5 6
    Set-Pixel $image $cyan 10 6
    Set-Pixel $image $cyan 6 7
    Set-Pixel $image $cyan 9 7
    Fill-Rect $image $cyan 7 8 2 4
    Set-Pixel $image $ice 7 8
    Set-Pixel $image $ice 8 8
    Set-Pixel $image $star 11 4
    Set-Pixel $image $star 6 11
}

New-Texture 'item/infinity_leggings.png' 16 16 {
    param($image)
    Fill-Rect $image $violet 3 2 10 3
    Fill-Rect $image $obsidian 4 4 8 4
    Fill-Rect $image $shadow 4 7 3 7
    Fill-Rect $image $shadow 9 7 3 7
    Fill-Rect $image $cyan 5 7 1 6
    Fill-Rect $image $cyan 10 7 1 6
    Fill-Rect $image $brightViolet 7 5 2 5
    Set-Pixel $image $ice 7 6
    Set-Pixel $image $star 11 9
}

New-Texture 'item/infinity_boots.png' 16 16 {
    param($image)
    Fill-Rect $image $obsidian 3 3 4 8
    Fill-Rect $image $obsidian 9 3 4 8
    Fill-Rect $image $violet 2 10 5 3
    Fill-Rect $image $violet 9 10 5 3
    Fill-Rect $image $cyan 4 5 1 6
    Fill-Rect $image $cyan 11 5 1 6
    Set-Pixel $image $brightViolet 2 7
    Set-Pixel $image $brightViolet 13 7
    Set-Pixel $image $ice 4 10
    Set-Pixel $image $ice 11 10
}
