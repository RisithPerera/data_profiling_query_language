import json
import sys
import os
import html as html_module

def parse_node(obj, parent_lhs=None):
    lhs = list(parent_lhs) if parent_lhs else []
    node_val = obj.get("node", "root")
    if node_val != "root":
        lhs = lhs + [node_val]
    node = {
        "id": "node_" + "_".join(str(x) for x in lhs) if lhs else "node_root",
        "node": node_val,
        "lhs": lhs,
        "prop":  obj.get("prop",  []),
        "cand":  obj.get("cand",  []),
        "valid": obj.get("valid", []),
        "children": []
    }
    for child in obj.get("children", []):
        node["children"].append(parse_node(child, lhs))
    return node


def compute_layout(node, depth=0, counter=[0]):
    if not node["children"]:
        node["_x"] = counter[0]
        node["_y"] = depth
        counter[0] += 1
        return 1
    total = 0
    for child in node["children"]:
        total += compute_layout(child, depth + 1, counter)
    node["_x"] = (node["children"][0]["_x"] + node["children"][-1]["_x"]) / 2
    node["_y"] = depth
    return total


def collect_nodes(node, result=None):
    if result is None:
        result = []
    result.append(node)
    for child in node["children"]:
        collect_nodes(child, result)
    return result


def build_svg_content(root):
    compute_layout(root)
    all_nodes = collect_nodes(root)

    max_depth = max(n["_y"] for n in all_nodes)
    max_x     = max(n["_x"] for n in all_nodes)

    all_indices = set()
    for n in all_nodes:
        all_indices.update(n["prop"])
        all_indices.update(n["cand"])
        all_indices.update(n["valid"])
    num_bits = max(all_indices) + 1 if all_indices else 9

    CELL      = 14
    GAP       = 2
    STEP      = CELL + GAP
    GRID_W    = num_bits * STEP - GAP
    GRID_H    = 3 * STEP - GAP
    NODE_R    = 16
    ABOVE_GAP = 6
    TOTAL_ABOVE = GRID_H + ABOVE_GAP + NODE_R

    H_SPACING = max(GRID_W + 36, 130)
    V_SPACING = TOTAL_ABOVE + NODE_R + 32

    LEFT_PAD = max(70, GRID_W // 2 + 28)
    TOP_PAD  = 50

    SVG_W = max(600, int((max_x + 1) * H_SPACING) + LEFT_PAD * 2)
    SVG_H = int((max_depth + 1) * V_SPACING) + TOP_PAD + 40

    def px(n):
        x = LEFT_PAD + n["_x"] * H_SPACING
        y = TOP_PAD  + n["_y"] * V_SPACING + TOTAL_ABOVE
        return x, y

    edges_s = []
    nodes_s = []
    ROW_KEYS = ["prop", "cand", "valid"]

    for n in all_nodes:
        x, y = px(n)

        for child in n["children"]:
            cx, cy = px(child)
            child_grid_top = cy - TOTAL_ABOVE
            edges_s.append(
                f'<line x1="{x:.1f}" y1="{y + NODE_R:.1f}" '
                f'x2="{cx:.1f}" y2="{child_grid_top:.1f}" class="edge"/>'
            )

        gx = x - GRID_W / 2
        gy = y - TOTAL_ABOVE

        for row_i, key in enumerate(ROW_KEYS):
            bits = set(n[key])
            for col_i in range(num_bits):
                rx = gx + col_i * STEP
                ry = gy + row_i * STEP
                is_set = col_i in bits
                css = f"gc gc-{key}" if is_set else "gc gc-off"
                nodes_s.append(
                    f'<rect x="{rx:.1f}" y="{ry:.1f}" '
                    f'width="{CELL}" height="{CELL}" rx="2" class="{css}"/>'
                )
                if is_set:
                    nodes_s.append(
                        f'<text x="{rx + CELL/2:.1f}" y="{ry + CELL/2:.1f}" '
                        f'dominant-baseline="central" text-anchor="middle" '
                        f'class="gv gv-{key}">{col_i}</text>'
                    )

        if n["node"] == "root":
            circle_cls = "nc-root"
        elif n["valid"]:
            circle_cls = "nc-valid"
        elif n["cand"]:
            circle_cls = "nc-cand"
        else:
            circle_cls = "nc-def"

        node_label = str(n["node"]) if n["node"] != "root" else "R"
        lhs_str = "{" + ",".join(str(a) for a in n["lhs"]) + "}" if n["lhs"] else "∅"

        nodes_s.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{NODE_R}" class="{circle_cls}"/>')
        nodes_s.append(
            f'<text x="{x:.1f}" y="{y:.1f}" dominant-baseline="central" '
            f'text-anchor="middle" class="nl">{html_module.escape(node_label)}</text>'
        )
        nodes_s.append(
            f'<text x="{x:.1f}" y="{y + NODE_R + 10:.1f}" '
            f'dominant-baseline="hanging" text-anchor="middle" '
            f'class="ll">{html_module.escape(lhs_str)}</text>'
        )

    svg_inner = "".join(edges_s) + "".join(nodes_s)
    return SVG_W, SVG_H, svg_inner, len(all_nodes), num_bits


def render_html(root, output_path):
    SVG_W, SVG_H, svg_inner, node_count, num_bits = build_svg_content(root)

    # Escape for embedding in JS string
    svg_inner_js = svg_inner.replace('\\', '\\\\').replace('`', '\\`').replace('$', '\\$')

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>PositiveCoverNode Tree</title>
<style>
*,*::before,*::after{{box-sizing:border-box;margin:0;padding:0}}
html,body{{height:100%;overflow:hidden;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;background:#f8f7f4;color:#1a1a18}}

header{{padding:11px 20px;border-bottom:1px solid #d0cfc7;background:#fff;display:flex;align-items:center;gap:12px;flex-shrink:0}}
header h1{{font-size:14px;font-weight:500;color:#1a1a18}}
#file-info{{font-size:11px;color:#888}}
.hint{{margin-left:auto;font-size:11px;color:#aaa}}

.load-btn{{
  display:inline-flex;align-items:center;gap:6px;
  padding:5px 12px;border-radius:6px;border:1px solid #d0cfc7;
  background:#fff;color:#444;font-size:12px;cursor:pointer;
  transition:background .15s,border-color .15s;white-space:nowrap;
}}
.load-btn:hover{{background:#f1f0ea;border-color:#b0ae a6}}
.load-btn svg{{flex-shrink:0}}

.legend{{display:flex;gap:14px;padding:7px 20px;border-bottom:1px solid #d0cfc7;background:#fff;font-size:10px;align-items:center;flex-wrap:wrap;flex-shrink:0}}
.li{{display:flex;align-items:center;gap:4px;color:#444}}
.ld{{width:10px;height:10px;border-radius:2px;flex-shrink:0}}

.canvas-wrap{{flex:1;overflow:hidden;cursor:grab;user-select:none;background:#f8f7f4;position:relative}}
.canvas-wrap:active{{cursor:grabbing}}

#empty{{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px;color:#bbb;font-size:13px;pointer-events:none}}
#empty svg{{opacity:.35}}

svg#svg{{display:block;transform-origin:0 0}}

.edge{{stroke:#c8c6be;stroke-width:1;fill:none}}
.gc{{stroke:none}}
.gc-off  {{fill:#eceae2}}
.gc-prop {{fill:#bfdbfe}}
.gc-cand {{fill:#fde68a}}
.gc-valid{{fill:#bbf7d0}}
.gv{{font-size:7px;font-weight:600;pointer-events:none}}
.gv-prop {{fill:#1d4ed8}}
.gv-cand {{fill:#92400e}}
.gv-valid{{fill:#166534}}
.nc-root {{fill:#f1f0ea;stroke:#aaa;stroke-width:1.2}}
.nc-def  {{fill:#eff6ff;stroke:#93c5fd;stroke-width:1.2}}
.nc-cand {{fill:#fffbeb;stroke:#fbbf24;stroke-width:1.5}}
.nc-valid{{fill:#f0fdf4;stroke:#4ade80;stroke-width:1.8}}
.nl{{font-size:10px;font-weight:600;fill:#1a1a18;pointer-events:none}}
.ll{{font-size:8px;fill:#999;pointer-events:none}}
</style>
</head>
<body style="display:flex;flex-direction:column;height:100%">

<header>
  <h1>PositiveCoverNode Tree</h1>
  <span id="file-info">{node_count} nodes · {num_bits} attributes</span>
  <label class="load-btn" title="Load JSON file">
    <svg width="13" height="13" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
      <path d="M2 10v3a1 1 0 001 1h10a1 1 0 001-1v-3M8 2v8M5 5l3-3 3 3"/>
    </svg>
    Load JSON
    <input type="file" accept=".json" id="file-input" style="display:none">
  </label>
  <span class="hint">Scroll to zoom · Drag to pan</span>
</header>

<div class="legend">
  <span style="color:#888">Grid rows (top→bottom):</span>
  <div class="li"><div class="ld" style="background:#bfdbfe;outline:1px solid #93c5fd"></div> rhsAttributes</div>
  <div class="li"><div class="ld" style="background:#fde68a;outline:1px solid #fbbf24"></div> rhsCandidateFds</div>
  <div class="li"><div class="ld" style="background:#bbf7d0;outline:1px solid #4ade80"></div> rhsValidatedFds</div>
  <span style="margin-left:10px;color:#888">Node:</span>
  <div class="li"><div class="ld" style="background:#eff6ff;outline:1px solid #93c5fd"></div> default</div>
  <div class="li"><div class="ld" style="background:#fffbeb;outline:1px solid #fbbf24"></div> has candidates</div>
  <div class="li"><div class="ld" style="background:#f0fdf4;outline:1px solid #4ade80"></div> has valid FDs</div>
</div>

<div class="canvas-wrap" id="wrap">
  <div id="empty">
    <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#aaa" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round">
      <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
    </svg>
    <span>Click <b>Load JSON</b> to open a tree file</span>
  </div>
  <svg id="svg" viewBox="0 0 {SVG_W} {SVG_H}" width="{SVG_W}" height="{SVG_H}" xmlns="http://www.w3.org/2000/svg">
  {svg_inner}
  </svg>
</div>

<script>
const wrap = document.getElementById('wrap');
const svgEl = document.getElementById('svg');
const fileInfo = document.getElementById('file-info');
const emptyEl = document.getElementById('empty');

let scale = 1, tx = 0, ty = 0;
let dragging = false, sx = 0, sy = 0, stx = 0, sty = 0;

function apply() {{
  svgEl.style.transform = `translate(${{tx}}px,${{ty}}px) scale(${{scale}})`;
}}

function fitToWindow(w, h) {{
  const ww = wrap.clientWidth, wh = wrap.clientHeight;
  scale = Math.min(ww / w, wh / h, 1);
  tx = (ww - w * scale) / 2;
  ty = 20;
  apply();
}}

window.addEventListener('load', () => {{
  fitToWindow({SVG_W}, {SVG_H});
}});

// File picker
document.getElementById('file-input').addEventListener('change', e => {{
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = ev => {{
    try {{
      const data = JSON.parse(ev.target.result);
      loadTree(data, file.name);
    }} catch(err) {{
      alert('Invalid JSON: ' + err.message);
    }}
  }};
  reader.readAsText(file);
  e.target.value = '';
}});

function loadTree(data, filename) {{
  // Build SVG via the embedded renderer (re-uses Python logic via inline JS port)
  const result = buildSVG(data);
  svgEl.setAttribute('viewBox', `0 0 ${{result.w}} ${{result.h}}`);
  svgEl.setAttribute('width',  result.w);
  svgEl.setAttribute('height', result.h);
  svgEl.innerHTML = result.inner;
  emptyEl.style.display = 'none';
  svgEl.style.display = 'block';
  fileInfo.textContent = `${{result.nodeCount}} nodes · ${{result.numBits}} attributes · ${{filename}}`;
  fitToWindow(result.w, result.h);
}}

// ── JS port of the Python layout + SVG builder ──────────────────────────────

function parseNode(obj, parentLhs) {{
  parentLhs = parentLhs || [];
  const nodeVal = obj.node !== undefined ? obj.node : 'root';
  const lhs = nodeVal !== 'root' ? [...parentLhs, nodeVal] : [...parentLhs];
  const id = lhs.length ? 'node_' + lhs.join('_') : 'node_root';
  return {{
    id, node: nodeVal, lhs,
    prop:  obj.prop  || [],
    cand:  obj.cand  || [],
    valid: obj.valid || [],
    children: (obj.children || []).map(c => parseNode(c, lhs))
  }};
}}

function computeLayout(node, depth, counter) {{
  depth = depth || 0;
  if (!node.children.length) {{
    node._x = counter[0]++;
    node._y = depth;
    return 1;
  }}
  let total = 0;
  for (const c of node.children) total += computeLayout(c, depth + 1, counter);
  node._x = (node.children[0]._x + node.children[node.children.length-1]._x) / 2;
  node._y = depth;
  return total;
}}

function collectNodes(node, arr) {{
  arr = arr || [];
  arr.push(node);
  for (const c of node.children) collectNodes(c, arr);
  return arr;
}}

function esc(s) {{
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}}

function buildSVG(data) {{
  const root = parseNode(data);
  computeLayout(root, 0, [0]);
  const all = collectNodes(root);

  const maxDepth = Math.max(...all.map(n => n._y));
  const maxX     = Math.max(...all.map(n => n._x));

  const allIdx = new Set();
  for (const n of all) {{
    n.prop.forEach(i => allIdx.add(i));
    n.cand.forEach(i => allIdx.add(i));
    n.valid.forEach(i => allIdx.add(i));
  }}
  const numBits = allIdx.size ? Math.max(...allIdx) + 1 : 9;

  const CELL=14, GAP=2, STEP=CELL+GAP;
  const GRID_W = numBits*STEP - GAP;
  const GRID_H = 3*STEP - GAP;
  const NODE_R = 16, ABOVE_GAP = 6;
  const TOTAL_ABOVE = GRID_H + ABOVE_GAP + NODE_R;
  const H_SPACING = Math.max(GRID_W + 36, 130);
  const V_SPACING = TOTAL_ABOVE + NODE_R + 32;
  const LEFT_PAD = Math.max(70, Math.floor(GRID_W/2) + 28);
  const TOP_PAD  = 50;
  const SVG_W = Math.max(600, Math.ceil((maxX+1)*H_SPACING) + LEFT_PAD*2);
  const SVG_H = Math.ceil((maxDepth+1)*V_SPACING) + TOP_PAD + 40;

  function px(n) {{
    return [LEFT_PAD + n._x*H_SPACING, TOP_PAD + n._y*V_SPACING + TOTAL_ABOVE];
  }}

  let out = '';
  const ROWS = ['prop','cand','valid'];

  for (const n of all) {{
    const [x,y] = px(n);
    for (const child of n.children) {{
      const [cx,cy] = px(child);
      out += `<line x1="${{x.toFixed(1)}}" y1="${{(y+NODE_R).toFixed(1)}}" x2="${{cx.toFixed(1)}}" y2="${{(cy-TOTAL_ABOVE).toFixed(1)}}" class="edge"/>`;
    }}
    const gx = x - GRID_W/2, gy = y - TOTAL_ABOVE;
    for (let ri=0; ri<ROWS.length; ri++) {{
      const bits = new Set(n[ROWS[ri]]);
      for (let ci=0; ci<numBits; ci++) {{
        const rx2 = gx + ci*STEP, ry2 = gy + ri*STEP;
        const set = bits.has(ci);
        out += `<rect x="${{rx2.toFixed(1)}}" y="${{ry2.toFixed(1)}}" width="${{CELL}}" height="${{CELL}}" rx="2" class="gc ${{set ? 'gc-'+ROWS[ri] : 'gc-off'}}"/>`;
        if (set) {{
          out += `<text x="${{(rx2+CELL/2).toFixed(1)}}" y="${{(ry2+CELL/2).toFixed(1)}}" dominant-baseline="central" text-anchor="middle" class="gv gv-${{ROWS[ri]}}">${{ci}}</text>`;
        }}
      }}
    }}
    const cls = n.valid.length ? 'nc-valid' : n.cand.length ? 'nc-cand' : n.node==='root' ? 'nc-root' : 'nc-def';
    const label = n.node === 'root' ? 'R' : String(n.node);
    const lhs = n.lhs.length ? '{{'+n.lhs.join(',')+'}}' : '∅';
    out += `<circle cx="${{x.toFixed(1)}}" cy="${{y.toFixed(1)}}" r="${{NODE_R}}" class="${{cls}}"/>`;
    out += `<text x="${{x.toFixed(1)}}" y="${{y.toFixed(1)}}" dominant-baseline="central" text-anchor="middle" class="nl">${{esc(label)}}</text>`;
    out += `<text x="${{x.toFixed(1)}}" y="${{(y+NODE_R+10).toFixed(1)}}" dominant-baseline="hanging" text-anchor="middle" class="ll">${{esc(lhs)}}</text>`;
  }}

  return {{ w: SVG_W, h: SVG_H, inner: out, nodeCount: all.length, numBits }};
}}

// ── Pan / zoom ───────────────────────────────────────────────────────────────

wrap.addEventListener('wheel', e => {{
  e.preventDefault();
  const rect = wrap.getBoundingClientRect();
  const mx = e.clientX - rect.left, my = e.clientY - rect.top;
  const delta = e.deltaY < 0 ? 1.12 : 1/1.12;
  const ns = Math.min(Math.max(scale * delta, 0.08), 10);
  tx = mx - (mx - tx) * (ns / scale);
  ty = my - (my - ty) * (ns / scale);
  scale = ns;
  apply();
}}, {{passive:false}});

wrap.addEventListener('mousedown', e => {{
  dragging = true; sx = e.clientX; sy = e.clientY; stx = tx; sty = ty;
  wrap.style.cursor = 'grabbing';
}});
window.addEventListener('mousemove', e => {{
  if (!dragging) return;
  tx = stx + (e.clientX - sx); ty = sty + (e.clientY - sy); apply();
}});
window.addEventListener('mouseup', () => {{ dragging = false; wrap.style.cursor = 'grab'; }});

let lastDist = null;
wrap.addEventListener('touchstart', e => {{
  if (e.touches.length===1) {{ sx=e.touches[0].clientX; sy=e.touches[0].clientY; stx=tx; sty=ty; }}
  else if (e.touches.length===2) {{ lastDist=Math.hypot(e.touches[0].clientX-e.touches[1].clientX,e.touches[0].clientY-e.touches[1].clientY); }}
}},{{passive:true}});
wrap.addEventListener('touchmove', e => {{
  e.preventDefault();
  if (e.touches.length===1) {{ tx=stx+(e.touches[0].clientX-sx); ty=sty+(e.touches[0].clientY-sy); apply(); }}
  else if (e.touches.length===2) {{
    const d=Math.hypot(e.touches[0].clientX-e.touches[1].clientX,e.touches[0].clientY-e.touches[1].clientY);
    scale=Math.min(Math.max(scale*(d/lastDist),0.08),10); lastDist=d; apply();
  }}
}},{{passive:false}});
</script>
</body>
</html>
"""

    with open(output_path, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"✓ {output_path}  ({node_count} nodes, {num_bits} attributes)")
    print(f"  Open: file://{os.path.abspath(output_path)}")


def main():
    if len(sys.argv) < 2:
        # No args — generate a blank viewer
        output_path = "../backup/visualizers/tree_viewer.html"
        # Build a minimal empty root so the template renders
        root = {"node": "root", "prop": [], "cand": [], "valid": [], "children": []}
        root_node = parse_node(root)

        class _FakeRoot:
            pass
        r = _FakeRoot()
        r.__dict__.update(root_node)
        r.__dict__['_x'] = 0
        r.__dict__['_y'] = 0

        # We still need build_svg_content to work; just produce a minimal SVG
        import types
        fake = types.SimpleNamespace(**root_node)
        fake._x = 0
        fake._y = 0

        # Just call render_html with real parse
        real_root = parse_node({"node": "root", "prop": [], "cand": [], "valid": [], "children": []})
        render_html(real_root, output_path)
        return

    input_path  = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else os.path.splitext(input_path)[0] + ".html"
    with open(input_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    root = parse_node(data)
    render_html(root, output_path)

if __name__ == "__main__":
    main()