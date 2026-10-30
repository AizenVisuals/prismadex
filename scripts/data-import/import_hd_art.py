#!/usr/bin/env python3
"""HD art import for PrismaDex detail/viewer only (256px list/grid untouched).

Sources (verified only, pinned commit): PokeAPI/sprites official-artwork
(native 475x475) for normal art + PokeAPI-fallback shinys; TonOfDirt726
DeviantArt originals (wixmp base URL, no /v1/ preview) for recolor shinys.
No datadex, no PokemonDB, no hue-shift, no upscaling of 256px files.
Output: assets/art-hd/<id>.webp (475 canvas, reframed for Mega/Gmax),
assets/art-hd/shiny/<id>.webp (475 canvas PokeAPI / 512 canvas DA),
each normalised into the form's own content box so detail-header size
and framing match the 256px assets exactly (proportional trim scaling).
Usage: python3 import_hd_art.py [--limit N] [--ids 1,25,...]
"""
import argparse, concurrent.futures, hashlib, json, os, re, subprocess, sys, time, urllib.request, urllib.error
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "android/app/src/main/assets"
SHA = "1dd55c0436d2a5b6e324c0b780e1b1b416fbfb64"
CACHE = Path("/tmp/hd-download-cache")
CACHE.mkdir(exist_ok=True)
UA = {"User-Agent": "PrismaDex-data-import"}
OUT_HD = ASSETS / "art-hd"
OUT_SH = ASSETS / "art-hd" / "shiny"


def fetch(url, dest, retries=3):
    if dest.exists() and dest.stat().st_size > 0:
        return dest.read_bytes()
    last = None
    for i in range(retries):
        try:
            req = urllib.request.Request(url, headers=UA)
            with urllib.request.urlopen(req, timeout=30) as r:
                raw = r.read()
            if not raw:
                raise ValueError("empty response")
            dest.write_bytes(raw)
            return raw
        except Exception as e:
            last = e
            time.sleep(1 + i)
    raise RuntimeError(f"fetch failed {url}: {last}")


def sha256(b):
    return hashlib.sha256(b).hexdigest()


def trim_of(path):
    out = subprocess.check_output(
        ["magick", "identify", "-format", "%@", str(path)], text=True, stderr=subprocess.DEVNULL
    ).strip()
    m = re.match(r"(\d+)x(\d+)\+(\d+)\+(\d+)", out)
    if not m:
        raise ValueError(f"bad trim {out!r} for {path}")
    return tuple(int(x) for x in m.groups())  # w,h,x,y


def run_magick(args):
    subprocess.run(["magick"] + args, check=True, stderr=subprocess.PIPE)


def compose(src, trim, box, canvas, out, bottom=False):
    """Crop src to trim, fit inside box (x,y,w,h), paste on transparent canvas."""
    w, h, x, y = trim
    bx, by, bw, bh = box
    scale = min(bw / w, bh / h)
    nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
    px = bx + (bw - nw) // 2
    py = by + bh - nh if bottom else by + (bh - nh) // 2
    tmp = Path(f"/tmp/hd-tmp-{os.getpid()}-{out.stem}.png")
    try:
        run_magick([str(src), "-crop", f"{w}x{h}+{x}+{y}", "+repage",
                    "-filter", "Lanczos", "-resize", f"{nw}x{nh}", str(tmp)])
        run_magick(["-size", f"{canvas}x{canvas}", "xc:none", str(tmp),
                    "-geometry", f"+{px}+{py}", "-composite",
                    "-strip", "-quality", "85", str(out)])
    finally:
        if tmp.exists():
            tmp.unlink()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--ids", default="")
    ap.add_argument("--only-missing", action="store_true",
                    help="process only ids listed as missing in the previous manifest, merging into it")
    args = ap.parse_args()

    trim256 = {int(k): tuple(v) for k, v in json.loads((ASSETS / "art-trim.json").read_text()).items()}
    data = json.loads((ASSETS / "data.json").read_text())
    extra = json.loads((ASSETS / "additional-form-profiles.json").read_text())["records"]
    fmap = json.loads((Path(ROOT) / "scripts/data-import/form-id-map.json").read_text())
    meta = {}
    for r in data:
        meta[r["id"]] = {"pokeapi": int(fmap.get(str(r["id"]), {}).get("pokeapiId", r["id"])),
                         "base": r["baseId"],
                         "mega": bool(r["isMega"]), "gmax": r["name"].startswith("Gigantamax ")}
    for r in extra:
        meta[r["id"]] = {"pokeapi": int(fmap.get(str(r["id"]), {}).get("pokeapiId", r["pokeapiId"])),
                         "base": r["baseId"],
                         "mega": bool(r["isMega"]), "gmax": r["name"].startswith("Gigantamax ")}
    art_ids = sorted(int(f[:-5]) for f in os.listdir(ASSETS / "art") if f.endswith(".webp"))
    shiny_src = json.loads((ASSETS / "shiny-art-sources.json").read_text())["records"]
    imp = json.loads((Path(ROOT) / "research/shiny-completion/import-manifest.json").read_text())
    wixmp = {}
    for r in imp.get("records", []):
        if r.get("provider") == "TonOfDirt726" and r.get("imageUrl"):
            wixmp[r["id"]] = r["imageUrl"].split("/v1/")[0]
    if args.ids:
        wanted = {int(x) for x in args.ids.split(",") if x.strip()}
        art_ids = [i for i in art_ids if i in wanted]

    OUT_HD.mkdir(exist_ok=True)
    OUT_SH.mkdir(exist_ok=True)
    manifest = {"sourceCommit": SHA, "quality": 85, "records": {}, "missing": []}
    prev_manifest = None
    if args.only_missing:
        prev_path = Path(ROOT) / "research/HD-ART-IMPORT-MANIFEST.json"
        if prev_path.exists():
            prev_manifest = json.loads(prev_path.read_text())
            manifest["records"] = prev_manifest.get("records", {})
            miss_n = {x["id"] for x in prev_manifest.get("missing", []) if x["kind"] == "normal"}
            miss_s = {x["id"] for x in prev_manifest.get("missing", []) if x["kind"] == "shiny"}
            art_ids = [i for i in art_ids if i in miss_n]
            # shiny_ids is computed later from shiny_src; restrict there
            manifest["_only_missing_shiny"] = sorted(miss_s)

    # --- Phase 1: download PokeAPI normal PNGs (parallel) ---
    def dl_normal(i):
        m = meta.get(i)
        if not m:
            return i, None, "no-meta"
        url = f"https://raw.githubusercontent.com/PokeAPI/sprites/{SHA}/sprites/pokemon/other/official-artwork/{m['pokeapi']}.png"
        dest = CACHE / f"n-{m['pokeapi']}.png"
        try:
            raw = fetch(url, dest)
            if not raw.startswith(b"\x89PNG"):
                return i, None, "not-png"
            return i, dest, sha256(raw)
        except Exception as e:
            return i, None, str(e)[:120]

    dl_ids = art_ids[: args.limit] if args.limit else art_ids
    normals = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        for i, dest, info in pool.map(dl_normal, dl_ids):
            normals[i] = (dest, info)
    print(f"normal downloads: {sum(1 for v in normals.values() if v[0])}/{len(dl_ids)}", flush=True)

    # HD trims of normal art (needed for reframing + shiny normalisation)
    def get_trim(item):
        i, (dest, info) = item
        if not dest:
            return i, None
        try:
            return i, trim_of(dest)
        except Exception as e:
            return i, str(e)

    hd_trims = {}
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        for i, t in pool.map(get_trim, normals.items()):
            if isinstance(t, tuple):
                hd_trims[i] = t
    print(f"normal HD trims: {len(hd_trims)}", flush=True)

    def scaled_box(i, canvas):
        """App-established content box (art-trim.json, 256 coords) scaled to
        canvas coords, returned as (x, y, w, h). Guarantees HD framing ==
        256px framing proportionally."""
        t = trim256.get(i)
        if not t:
            return None
        s = canvas / 256.0
        return (round(t[0] * s), round(t[1] * s), round(t[2] * s), round(t[3] * s))

    def base_box(i):
        """Post-reframe content box of form i in 475 canvas coords:
        the base form's scaled trim box for Mega/Gmax, own box otherwise."""
        m = meta[i]
        if m["mega"] or m["gmax"]:
            return scaled_box(m["base"], 475) or scaled_box(i, 475)
        return scaled_box(i, 475)

    # --- Phase 2: write normal HD ---
    for i in dl_ids:
        dest, info = normals[i]
        rec = {"id": i, "provider": "PokeAPI official-artwork", "sourceSha256": info}
        if not dest:
            rec["hdNormal"] = {"status": "missing", "reason": info}
            manifest["records"].setdefault(str(i), {}).update(rec)
            manifest["missing"].append({"id": i, "kind": "normal", "reason": info})
            continue
        out = OUT_HD / f"{i}.webp"
        m = meta[i]
        if m["mega"] or m["gmax"]:
            box = base_box(i)  # (x,y,w,h) in 475 coords
            t = hd_trims[i]
            compose(dest, t, box, 475, out, bottom=True)
            rec["hdNormal"] = {"status": "ok", "procedure": "reframed-to-base-box",
                               "canvas": 475, "box": list(box)}
        else:
            run_magick([str(dest), "-strip", "-quality", "85", str(out)])
            rec["hdNormal"] = {"status": "ok", "procedure": "full-native-canvas", "canvas": 475}
        rec["hdNormal"]["bytes"] = out.stat().st_size
        rec["hdNormal"]["sha256"] = sha256(out.read_bytes())
        manifest["records"].setdefault(str(i), {}).update(rec)
    print("normal HD written", flush=True)

    # DA page fallback for the original 94 (no wixmp URL in the manifest):
    # fetch the deviation page (public, no login) and take the base wixmp
    # image URL (strip /v1/ preview suffix) -> the full original.
    _da_cache_path = Path("/tmp/da-wixmp-cache.json")
    _da_cache = json.loads(_da_cache_path.read_text()) if _da_cache_path.exists() else {}

    def resolve_da_url(i):
        if str(i) in _da_cache:
            return _da_cache[str(i)]
        page = shiny_src[str(i)].get("source", "")
        if not page:
            return None
        try:
            html = fetch(page, CACHE / f"da-page-{i}.html").decode("utf-8", "ignore")
        except Exception:
            return None
        # Prefer an /i/ original (no token needed); /f/ originals are
        # token-gated (401 without ?token=), so take the widest tokenized
        # /v1/ variant present in the page (up to 2560w) instead.
        cands = re.findall(r"https://images-wixmp-[^\s\"'<>]+?\.png[^\s\"'<>]*", html)
        base = [u for u in cands if "/i/" in u]
        if base:
            url = base[0].split("/v1/")[0]
        else:
            best, best_w = None, -1
            for u in cands:
                m = re.search(r"/v1/(?:fit|fill)/w_(\d+)", u)
                if m and int(m.group(1)) > best_w:
                    best, best_w = u, int(m.group(1))
            if not best:
                return None
            url = best
        _da_cache[str(i)] = url
        _da_cache_path.write_text(json.dumps(_da_cache))
        return url

    # --- Phase 3: shiny HD ---
    shiny_ids = sorted(int(k) for k in shiny_src.keys())
    if args.only_missing and prev_manifest is not None:
        shiny_ids = [i for i in shiny_ids if i in set(manifest.pop("_only_missing_shiny", []))]
    if args.ids:
        shiny_ids = [i for i in shiny_ids if i in set(art_ids)]
    if args.limit:
        shiny_ids = shiny_ids[: args.limit]

    def dl_shiny(i):
        prov = shiny_src[str(i)]["provider"]
        if prov.startswith("PokeAPI"):
            m = meta.get(i)
            if not m:
                return i, None, "no-meta", prov
            url = f"https://raw.githubusercontent.com/PokeAPI/sprites/{SHA}/sprites/pokemon/other/official-artwork/shiny/{m['pokeapi']}.png"
            dest = CACHE / f"s-{m['pokeapi']}.png"
            try:
                raw = fetch(url, dest)
                if not raw.startswith(b"\x89PNG"):
                    return i, None, "not-png", prov
                return i, dest, sha256(raw), prov
            except Exception as e:
                return i, None, str(e)[:120], prov
        # TonOfDirt726
        url = wixmp.get(i)
        if url and "/f/" in url:
            # /f/ originals are token-gated; use the page's widest
            # tokenized variant instead of the bare base URL (401).
            url = resolve_da_url(i) or url
        url = url or resolve_da_url(i)
        if not url:
            return i, None, "no-wixmp-url", prov
        dest = CACHE / f"da-{i}.png"
        try:
            raw = fetch(url, dest)
            if not (raw.startswith(b"\x89PNG") or raw.startswith(b"\xff\xd8")):
                return i, None, "not-image", prov
            return i, dest, sha256(raw), prov
        except Exception as e:
            return i, None, str(e)[:120], prov

    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        shiny_dl = {i: (d, info, prov) for i, d, info, prov in pool.map(dl_shiny, shiny_ids)}
    print(f"shiny downloads: {sum(1 for v in shiny_dl.values() if v[0])}/{len(shiny_ids)}", flush=True)

    for i in shiny_ids:
        dest, info, prov = shiny_dl[i]
        rec = manifest["records"].setdefault(str(i), {})
        if not dest:
            rec["hdShiny"] = {"status": "missing", "reason": info, "provider": prov}
            manifest["missing"].append({"id": i, "kind": "shiny", "reason": info})
            continue
        # target box: form's post-reframe content box at this canvas
        # (art-trim.json scaled), so shiny == normal size/framing.
        if prov.startswith("PokeAPI"):
            canvas = 475
            cbox = base_box(i)
        else:
            canvas = 512
            m2 = meta[i]
            cbox = scaled_box(m2["base"], 512) if (m2["mega"] or m2["gmax"]) else scaled_box(i, 512)
        if not cbox:
            rec["hdShiny"] = {"status": "missing", "reason": "no-normal-hd-box", "provider": prov}
            manifest["missing"].append({"id": i, "kind": "shiny", "reason": "no-normal-hd-box"})
            continue
        out = OUT_SH / f"{i}.webp"
        t = trim_of(dest)
        compose(dest, t, cbox, canvas, out, bottom=False)
        rec["hdShiny"] = {"status": "ok", "provider": prov, "canvas": canvas,
                          "bytes": out.stat().st_size, "sha256": sha256(out.read_bytes()),
                          "sourceSha256": info}
    print("shiny HD written", flush=True)

    manifest["summary"] = {
        "normalOk": sum(1 for r in manifest["records"].values() if r.get("hdNormal", {}).get("status") == "ok"),
        "shinyOk": sum(1 for r in manifest["records"].values() if r.get("hdShiny", {}).get("status") == "ok"),
        "missing": len(manifest["missing"]),
    }
    out_manifest = Path(ROOT) / "research/HD-ART-IMPORT-MANIFEST.json"
    out_manifest.write_text(json.dumps(manifest, ensure_ascii=False, indent=1) + "\n")
    print(json.dumps(manifest["summary"], indent=1))
    print("manifest:", out_manifest)


if __name__ == "__main__":
    main()
