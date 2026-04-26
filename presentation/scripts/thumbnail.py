"""
thumbnail.py — Render PPTX slides to a thumbnail grid using PowerPoint COM.

Usage:
  python scripts/thumbnail.py <pptx_file> [output_prefix] [--cols N]

Example:
  python scripts/thumbnail.py output/stylesense_presentation.pptx thumbnails --cols 5
"""

import sys
import os
import argparse
import pathlib
import win32com.client
from PIL import Image

def export_slides(pptx_path: str, slides_dir: str, width_px: int = 1280) -> list[str]:
    """Export each slide to PNG via PowerPoint COM. Returns list of PNG paths."""
    pptx_abs = str(pathlib.Path(pptx_path).resolve())
    slides_dir = pathlib.Path(slides_dir)
    slides_dir.mkdir(parents=True, exist_ok=True)

    print(f"Opening {pptx_abs} in PowerPoint…")
    ppt = win32com.client.Dispatch("PowerPoint.Application")
    ppt.Visible = True  # must be visible on Windows for export

    prs = ppt.Presentations.Open(pptx_abs, ReadOnly=True, Untitled=False, WithWindow=False)

    slide_w = prs.PageSetup.SlideWidth   # points
    slide_h = prs.PageSetup.SlideHeight  # points
    aspect  = slide_h / slide_w
    height_px = int(width_px * aspect)

    paths = []
    n = prs.Slides.Count
    print(f"Exporting {n} slides ({width_px}×{height_px}px each)…")

    for i in range(1, n + 1):
        out = str((slides_dir / f"slide_{i:02d}.png").resolve())
        prs.Slides(i).Export(out, "PNG", width_px, height_px)
        paths.append(out)
        print(f"  slide {i:02d}/{n}", end="\r")

    prs.Close()
    ppt.Quit()
    print(f"\nOK: Exported {n} slides to {slides_dir}")
    return paths


def build_grid(slide_paths: list[str], output_path: str, cols: int = 5) -> None:
    """Composite all slide PNGs into a single grid image."""
    imgs = [Image.open(p) for p in slide_paths]
    if not imgs:
        print("No slides to composite.")
        return

    tw, th = imgs[0].size
    rows = (len(imgs) + cols - 1) // cols
    pad  = 8
    grid_w = cols * tw + (cols + 1) * pad
    grid_h = rows * th + (rows + 1) * pad

    canvas = Image.new("RGB", (grid_w, grid_h), (20, 20, 30))

    for idx, img in enumerate(imgs):
        col = idx % cols
        row = idx // cols
        x = pad + col * (tw + pad)
        y = pad + row * (th + pad)
        canvas.paste(img, (x, y))

    canvas.save(output_path, quality=90)
    print(f"DONE: Thumbnail grid saved: {output_path}  ({grid_w}x{grid_h}px, {len(imgs)} slides)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("pptx_file",        help="Path to .pptx file")
    ap.add_argument("output_prefix",    nargs="?", default="thumbnails", help="Output prefix (default: thumbnails)")
    ap.add_argument("--cols",           type=int, default=5, help="Columns in grid (default: 5)")
    ap.add_argument("--width",          type=int, default=960, help="Slide render width in px (default: 960)")
    ap.add_argument("--keep-slides",    action="store_true", help="Keep individual slide PNGs")
    args = ap.parse_args()

    script_dir  = pathlib.Path(__file__).parent.parent  # presentation root
    pptx_path   = pathlib.Path(args.pptx_file)
    if not pptx_path.is_absolute():
        pptx_path = script_dir / pptx_path

    slides_dir  = script_dir / "_slide_pngs"
    output_path = script_dir / f"{args.output_prefix}.jpg"

    slide_paths = export_slides(str(pptx_path), str(slides_dir), args.width)
    build_grid(slide_paths, str(output_path), args.cols)

    if not args.keep_slides:
        import shutil
        shutil.rmtree(slides_dir, ignore_errors=True)


if __name__ == "__main__":
    main()
