import json, sys, os, io, base64, webbrowser, threading
from http.server import HTTPServer, BaseHTTPRequestHandler

from reportlab.pdfgen import canvas as rl_canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

# ── Font setup ────────────────────────────────────────────────────────────────
_FONT_CANDIDATES = [
    ("/usr/share/fonts/truetype/liberation/LiberationSerif-Regular.ttf",
     "/usr/share/fonts/truetype/liberation/LiberationSerif-Bold.ttf"),
    ("/usr/share/fonts/truetype/freefont/FreeSerif.ttf",
     "/usr/share/fonts/truetype/freefont/FreeSerifBold.ttf"),
]
_fonts_registered = False
FONT_REG  = "Times-Roman"
FONT_BOLD = "Times-Bold"

def _ensure_fonts():
    global _fonts_registered, FONT_REG, FONT_BOLD
    if _fonts_registered:
        return
    for reg_path, bold_path in _FONT_CANDIDATES:
        if os.path.exists(reg_path) and os.path.exists(bold_path):
            pdfmetrics.registerFont(TTFont("ThesisFont",      reg_path))
            pdfmetrics.registerFont(TTFont("ThesisFont-Bold", bold_path))
            FONT_REG  = "ThesisFont"
            FONT_BOLD = "ThesisFont-Bold"
            break
    _fonts_registered = True

# ── Tree parsing ──────────────────────────────────────────────────────────────
def parse_node(obj, parent_lhs=None):
    lhs = list(parent_lhs) if parent_lhs else []
    node_val = obj.get("node", "root")
    if node_val != "root":
        lhs = lhs + [node_val]
    node = {
        "node":  node_val, "lhs": lhs,
        "prop":  obj.get("prop",  []),
        "cand":  obj.get("cand",  []),
        "valid": obj.get("valid", []),
        "children": []
    }
    for child in obj.get("children", []):
        node["children"].append(parse_node(child, lhs))
    return node

def compute_layout(node, depth=0, counter=None):
    if counter is None:
        counter = [0]
    if not node["children"]:
        node["_x"] = counter[0]; node["_y"] = depth
        counter[0] += 1; return 1
    total = 0
    for c in node["children"]:
        total += compute_layout(c, depth + 1, counter)
    node["_x"] = (node["children"][0]["_x"] + node["children"][-1]["_x"]) / 2
    node["_y"] = depth
    return total

def collect_nodes(node, result=None):
    if result is None: result = []
    result.append(node)
    for c in node["children"]: collect_nodes(c, result)
    return result

# ── PDF generation → bytes ────────────────────────────────────────────────────
def build_pdf_bytes(root):
    _ensure_fonts()
    compute_layout(root)
    all_nodes = collect_nodes(root)

    all_idx = set()
    for n in all_nodes:
        all_idx.update(n["prop"]); all_idx.update(n["cand"]); all_idx.update(n["valid"])
    num_bits = max(all_idx) + 1 if all_idx else 9

    # ── Adjust these two to change diagram size ──────────────────────────────
    CELL        = 7.0    # grid cell size in pt  (increase → bigger grid boxes)
    NODE_R      = 10.0   # node circle radius in pt (increase → bigger circles)
    SHOW_LHS    = False  # set True to show {a,b,c} path labels below each node
    # ─────────────────────────────────────────────────────────────────────────

    GRID_W      = num_bits * CELL
    GRID_H      = 2 * CELL    #3 * CELL
    ABOVE_GAP   = 4.0
    TOTAL_ABOVE = GRID_H + ABOVE_GAP + NODE_R
    H_SPACING   = max(GRID_W + 14, NODE_R * 2 + 10)
    V_SPACING   = TOTAL_ABOVE + NODE_R + 18.0
    FONT_CELL   = CELL * 0.64          # scales with cell size
    FONT_NODE   = NODE_R * 0.60        # scales with circle size
    FONT_LHS    = 5.0
    MARGIN      = 4.0   # uniform margin around the tight bounding box

    # Origin: place the root node grid top at (0,0) in tree-space,
    # then compute every element's extent to find the true bounding box.
    def _node_pos_raw(n):
        # circle centre in tree-space (y increases downward)
        return (n["_x"] * H_SPACING,
                n["_y"] * V_SPACING + TOTAL_ABOVE)

    # True bounding box in tree-space
    xs_min, xs_max, ys_min, ys_max = [], [], [], []
    for n in all_nodes:
        tx, ty = _node_pos_raw(n)
        # grid top-left / bottom-right
        xs_min.append(tx - GRID_W / 2)
        xs_max.append(tx + GRID_W / 2)
        ys_min.append(ty - TOTAL_ABOVE)          # top of grid
        # bottom: circle bottom, plus LHS label height if shown
        lhs_extra = (FONT_LHS + 2) if SHOW_LHS else 0
        ys_max.append(ty + NODE_R + lhs_extra)

    bb_left  = min(xs_min)
    bb_right = max(xs_max)
    bb_top   = min(ys_min)
    bb_bot   = max(ys_max)

    content_w = bb_right - bb_left
    content_h = bb_bot   - bb_top
    page_w    = content_w + MARGIN * 2
    page_h    = content_h + MARGIN * 2

    # Shift so bb_left/bb_top maps to MARGIN in PDF tree-space
    ox = MARGIN - bb_left   # x offset
    oy = MARGIN - bb_top    # y offset

    def node_pos_off(n):
        tx, ty = _node_pos_raw(n)
        return tx + ox, ty + oy

    def to_pdf(tx, ty): return tx, page_h - ty

    node_pos = node_pos_off
    buf = io.BytesIO()
    c = rl_canvas.Canvas(buf, pagesize=(page_w, page_h))
    c.setLineWidth(0.35)

    FILL_OFF  = (0.96, 0.96, 0.94)
    FILL      = {"prop":(0.75,0.86,1.00), "cand":(1.00, 0.949, 0.80), "valid":(0.98, 0.81, 0.80)}
    TXT_CLR   = {"prop":(0.11,0.31,0.85), "cand":(0.57,0.25,0.05), "valid":(0.09,0.40,0.20)}
    CIRC_FILL = {"root":(0.95,0.94,0.92), "def":(0.94,0.96,1.00),
                 "cand":(1.00,0.98,0.92), "valid":(0.94,1.00,0.96)}
    ROWS = ["prop", "cand"] #["prop", "cand", "valid"]

    # Edges
    c.setStrokeColorRGB(0, 0, 0)
    for n in all_nodes:
        tx, ty = node_pos(n)
        px, py = to_pdf(tx, ty)
        for child in n["children"]:
            ctx, cty = node_pos(child)
            cpx, cpy = to_pdf(ctx, cty)
            c.line(px, py - NODE_R, cpx, cpy + TOTAL_ABOVE)

    # Nodes
    for n in all_nodes:
        tx, ty = node_pos(n)
        px, py = to_pdf(tx, ty)
        gx_t = tx - GRID_W / 2
        gy_t = ty - TOTAL_ABOVE

        for ri, key in enumerate(ROWS):
            bits = set(n[key])
            for ci in range(num_bits):
                rx_t = gx_t + ci * CELL
                ry_t = gy_t + ri * CELL
                fill = FILL[key] if ci in bits else FILL_OFF
                c.setFillColorRGB(*fill)
                c.setStrokeColorRGB(0, 0, 0)
                cell_y = page_h - ry_t - CELL
                c.rect(rx_t, cell_y, CELL, CELL, fill=1, stroke=1)
                if ci in bits:
                    c.setFillColorRGB(*TXT_CLR[key])
                    c.setFont(FONT_BOLD, FONT_CELL)
                    c.drawCentredString(rx_t + CELL/2, cell_y + (CELL - FONT_CELL)/2, str(ci))

        if   n["valid"]: ck = "valid"
        elif n["cand"]:  ck = "cand"
        elif n["node"] == "root": ck = "root"
        else: ck = "def"

        c.setFillColorRGB(*CIRC_FILL[ck])
        c.setStrokeColorRGB(0, 0, 0)
        c.circle(px, py, NODE_R, fill=1, stroke=1)

        label = str(n["node"]) if n["node"] != "root" else "[ ]"
        c.setFillColorRGB(0, 0, 0)
        c.setFont(FONT_BOLD, FONT_NODE)
        c.drawCentredString(px, py - FONT_NODE * 0.33, label)

        if SHOW_LHS:
            lhs_str = ("{" + ",".join(str(a) for a in n["lhs"]) + "}"
                       if n["lhs"] else "\u2205")
            c.setFont(FONT_REG, FONT_LHS)
            c.setFillColorRGB(0.35, 0.35, 0.35)
            c.drawCentredString(px, py - NODE_R - FONT_LHS - 1, lhs_str)

    c.showPage()
    c.save()
    return buf.getvalue()

# ── Shared state ──────────────────────────────────────────────────────────────
_state = {"pdf_bytes": None, "filename": "fd_tree.pdf"}

# ── HTTP handler ──────────────────────────────────────────────────────────────
class Handler(BaseHTTPRequestHandler):
    def log_message(self, *a): pass   # suppress access logs

    def do_GET(self):
        path = self.path.split("?")[0]
        if path == "/":
            self._serve_html()
        elif path == "/download":
            self._serve_download()
        else:
            self.send_error(404)

    def do_POST(self):
        if self.path == "/load":
            self._handle_load()
        else:
            self.send_error(404)

    # ── serve main page ───────────────────────────────────────────────────────
    def _serve_html(self):
        html = r"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>FD Tree PDF</title>
<style>
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
html, body {
  height: 100%; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  background: #f8f7f4; color: #1a1a18; display: flex; flex-direction: column;
}
header {
  padding: 10px 18px; background: #fff; border-bottom: 1px solid #d0cfc7;
  display: flex; align-items: center; gap: 10px; flex-shrink: 0;
}
header h1 { font-size: 14px; font-weight: 500; }
.spacer { flex: 1; }
.hint { font-size: 11px; color: #aaa; }
#status { font-size: 11px; color: #888; }

.btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 5px 13px; border-radius: 6px; border: 1px solid #d0cfc7;
  background: #fff; color: #444; font-size: 12px; cursor: pointer;
  font-family: inherit; white-space: nowrap;
  transition: background .15s, border-color .15s;
}
.btn:hover { background: #f1f0ea; border-color: #999; }
.btn-dl { border-color: #b6e2c8; color: #166534; background: #f0fdf4; }
.btn-dl:hover { background: #dcfce7; border-color: #4ade80; }
.btn-dl:disabled { opacity: 0.35; pointer-events: none; }

.preview {
  flex: 1; display: flex; align-items: center; justify-content: center;
  padding: 24px; overflow: auto;
}
#placeholder {
  display: flex; flex-direction: column; align-items: center; gap: 12px;
  color: #bbb; font-size: 13px;
}
#pdf-frame {
  display: none; width: 100%; height: 100%; border: none;
  border-radius: 4px; box-shadow: 0 2px 16px rgba(0,0,0,.12);
}
</style>
</head>
<body>
<header>
  <h1>FD Tree PDF Preview</h1>
  <span id="status">No file loaded</span>
  <div class="spacer"></div>

  <label class="btn">
    <svg width="13" height="13" viewBox="0 0 16 16" fill="none" stroke="currentColor"
         stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
      <path d="M2 10v3a1 1 0 001 1h10a1 1 0 001-1v-3M8 2v8M5 5l3-3 3 3"/>
    </svg>
    Load JSON
    <input type="file" accept=".json" id="file-input" style="display:none">
  </label>

  <a id="dl-btn" class="btn btn-dl" href="/download" download="fd_tree.pdf"
     style="pointer-events:none;opacity:0.35;text-decoration:none">
    <svg width="13" height="13" viewBox="0 0 16 16" fill="none" stroke="currentColor"
         stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
      <path d="M2 10v3a1 1 0 001 1h10a1 1 0 001-1v-3M8 2v8M5 9l3 3 3-3"/>
    </svg>
    Download PDF
  </a>
</header>

<div class="preview" id="preview-area">
  <div id="placeholder">
    <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#ccc"
         stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
      <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
      <polyline points="14 2 14 8 20 8"/>
    </svg>
    <span>Click <b>Load JSON</b> to generate a PDF preview</span>
  </div>
  <iframe id="pdf-frame"></iframe>
</div>

<script>
var dlBtn    = document.getElementById('dl-btn');
var status   = document.getElementById('status');
var frame    = document.getElementById('pdf-frame');
var placeholder = document.getElementById('placeholder');

document.getElementById('file-input').addEventListener('change', function (e) {
  var file = e.target.files[0];
  if (!file) return;
  status.textContent = 'Generating PDF…';
  dlBtn.style.pointerEvents = 'none';
  dlBtn.style.opacity = '0.35';

  var reader = new FileReader();
  reader.onload = function (ev) {
    var fd = new FormData();
    fd.append('filename', file.name);
    fd.append('json', ev.target.result);
    fetch('/load', { method: 'POST', body: fd })
      .then(function (r) {
        if (!r.ok) return r.text().then(function (t) { throw new Error(t); });
        return r.json();
      })
      .then(function (d) {
        // Show PDF in iframe via the /download endpoint
        frame.src = '/download?t=' + Date.now();
        frame.style.display = 'block';
        placeholder.style.display = 'none';
        dlBtn.setAttribute('download', d.filename);
        dlBtn.style.pointerEvents = '';
        dlBtn.style.opacity = '';
        status.textContent = d.info;
      })
      .catch(function (err) {
        alert('Error: ' + err.message);
        status.textContent = 'Error — see console';
      });
  };
  reader.readAsText(file);
  e.target.value = '';
});
</script>
</body>
</html>
"""
        body = html.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", len(body))
        self.end_headers()
        self.wfile.write(body)

    # ── serve PDF download ────────────────────────────────────────────────────
    def _serve_download(self):
        pdf = _state["pdf_bytes"]
        if not pdf:
            self.send_error(404, "No PDF generated yet")
            return
        self.send_response(200)
        self.send_header("Content-Type", "application/pdf")
        self.send_header("Content-Length", len(pdf))
        self.send_header("Content-Disposition",
                         f'inline; filename="{_state["filename"]}"')
        self.end_headers()
        self.wfile.write(pdf)

    # ── handle JSON upload → generate PDF ────────────────────────────────────
    def _handle_load(self):
        try:
            length = int(self.headers.get("Content-Length", 0))
            body   = self.rfile.read(length)

            # Parse multipart form data
            ct = self.headers.get("Content-Type", "")
            boundary = ct.split("boundary=")[-1].encode()
            parts = body.split(b"--" + boundary)

            fields = {}
            for part in parts[1:]:
                if b"\r\n\r\n" not in part: continue
                header, _, value = part.partition(b"\r\n\r\n")
                value = value.rstrip(b"\r\n--")
                header_str = header.decode("utf-8", errors="replace")
                if 'name="filename"' in header_str:
                    fields["filename"] = value.decode("utf-8")
                elif 'name="json"' in header_str:
                    fields["json"] = value.decode("utf-8")

            if "json" not in fields:
                raise ValueError("No JSON field in upload")

            data = json.loads(fields["json"])
            root = parse_node(data)
            pdf_bytes = build_pdf_bytes(root)

            fname = fields.get("filename", "fd_tree.json")
            out_name = os.path.splitext(fname)[0] + ".pdf"
            _state["pdf_bytes"] = pdf_bytes
            _state["filename"]  = out_name

            all_nodes = collect_nodes(root)
            all_idx = set()
            for n in all_nodes:
                all_idx.update(n["prop"]); all_idx.update(n["cand"]); all_idx.update(n["valid"])
            num_bits = max(all_idx) + 1 if all_idx else 9

            resp = json.dumps({
                "filename": out_name,
                "info": f"{len(all_nodes)} nodes · {num_bits} attributes · {out_name}"
            }).encode("utf-8")

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", len(resp))
            self.end_headers()
            self.wfile.write(resp)

        except Exception as e:
            err = str(e).encode("utf-8")
            self.send_response(500)
            self.send_header("Content-Type", "text/plain")
            self.send_header("Content-Length", len(err))
            self.end_headers()
            self.wfile.write(err)

# ── Entry point ───────────────────────────────────────────────────────────────
def main():
    port = 8742
    server = HTTPServer(("127.0.0.1", port), Handler)
    url = f"http://127.0.0.1:{port}"
    print(f"FD Tree PDF  →  {url}")
    print("Press Ctrl+C to stop.")
    threading.Timer(0.4, lambda: webbrowser.open(url)).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")

if __name__ == "__main__":
    main()
