import json
import sys
import os
import html as html_module


def parse_node(obj, parent_lhs=None):
    lhs = list(parent_lhs) if parent_lhs else []
    node_val = obj.get("node", "root")
    if node_val != "root":
        lhs = lhs + [node_val]
    return {
        "id":    "node_" + "_".join(str(x) for x in lhs) if lhs else "node_root",
        "node":  node_val,
        "lhs":   lhs,
        "cand":  bool(obj.get("cand",  False)),
        "valid": bool(obj.get("valid", False)),
        "children": [parse_node(c, lhs) for c in obj.get("children", [])]
    }


def compute_layout(node, depth=0, counter=[0]):
    if not node["children"]:
        node["_x"] = counter[0];  node["_y"] = depth
        counter[0] += 1
        return 1
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


def render_html(root, output_path):
    compute_layout(root)
    all_nodes = collect_nodes(root)

    max_depth = max(n["_y"] for n in all_nodes)
    max_x     = max(n["_x"] for n in all_nodes)
    node_count = len(all_nodes)

    # Two squares side-by-side above the circle
    # Square = 16x16, gap = 4 between them
    SQ       = 16          # square size px
    SQ_GAP   = 5           # gap between the two squares
    PAIR_W   = SQ * 2 + SQ_GAP   # total width of the two-square pair
    NODE_R   = 16
    ABOVE_GAP = 6
    TOTAL_ABOVE = SQ + ABOVE_GAP + NODE_R  # grid height + gap + circle radius

    H_SPACING = max(PAIR_W + 40, 90)
    V_SPACING = TOTAL_ABOVE + NODE_R + 30

    LEFT_PAD = max(60, PAIR_W // 2 + 24)
    TOP_PAD  = 50

    SVG_W = max(500, int((max_x + 1) * H_SPACING) + LEFT_PAD * 2)
    SVG_H = int((max_depth + 1) * V_SPACING) + TOP_PAD + 40

    def px(n):
        x = LEFT_PAD + n["_x"] * H_SPACING
        y = TOP_PAD  + n["_y"] * V_SPACING + TOTAL_ABOVE
        return x, y

    edges_s = []
    nodes_s = []

    for n in all_nodes:
        x, y = px(n)

        # Edges
        for child in n["children"]:
            cx, cy = px(child)
            child_top = cy - TOTAL_ABOVE
            edges_s.append(
                f'<line x1="{x:.1f}" y1="{y + NODE_R:.1f}" '
                f'x2="{cx:.1f}" y2="{child_top:.1f}" class="edge"/>'
            )

        # Two squares centered above circle
        pair_left = x - PAIR_W / 2
        sq_y      = y - TOTAL_ABOVE   # top of squares

        for i, (key, label) in enumerate([("cand", "C"), ("valid", "V")]):
            sx2 = pair_left + i * (SQ + SQ_GAP)
            is_set = n[key]
            sq_cls = f"sq sq-{key}" if is_set else "sq sq-off"
            nodes_s.append(
                f'<rect x="{sx2:.1f}" y="{sq_y:.1f}" '
                f'width="{SQ}" height="{SQ}" rx="3" class="{sq_cls}"/>'
            )
            if is_set:
                # filled: show letter in white
                nodes_s.append(
                    f'<text x="{sx2 + SQ/2:.1f}" y="{sq_y + SQ/2:.1f}" '
                    f'dominant-baseline="central" text-anchor="middle" '
                    f'class="sq-lbl sq-lbl-set">{label}</text>'
                )

        # Circle
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

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>UCCTreeNode Visualizer</title>
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
.load-btn:hover{{background:#f1f0ea;border-color:#b0aea6}}

.legend{{display:flex;gap:16px;padding:7px 20px;border-bottom:1px solid #d0cfc7;background:#fff;font-size:10px;align-items:center;flex-wrap:wrap;flex-shrink:0}}
.li{{display:flex;align-items:center;gap:5px;color:#444}}
.lsq{{width:13px;height:13px;border-radius:2px;flex-shrink:0;display:flex;align-items:center;justify-content:center;font-size:8px;font-weight:700}}

.canvas-wrap{{flex:1;overflow:hidden;cursor:grab;user-select:none;background:#f8f7f4;position:relative}}
.canvas-wrap:active{{cursor:grabbing}}

#empty{{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px;color:#bbb;font-size:13px;pointer-events:none}}
#empty svg{{opacity:.35}}

svg#svg{{display:block;transform-origin:0 0}}

/* Edges */
.edge{{stroke:#c8c6be;stroke-width:1;fill:none}}

/* Squares */
.sq{{stroke-width:1.2}}
.sq-off  {{fill:#fff;stroke:#ccc}}
.sq-cand {{fill:#fbbf24;stroke:#d97706}}
.sq-valid{{fill:#4ade80;stroke:#16a34a}}
.sq-lbl{{font-size:8px;font-weight:700;pointer-events:none}}
.sq-lbl-set{{fill:#fff}}

/* Circles */
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
  <h1>UCCTreeNode Positive Cover</h1>
  <span id="file-info">{node_count} nodes</span>
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
  <span style="color:#888">Squares above node (left→right):</span>
  <div class="li">
    <div class="lsq" style="background:#fbbf24;border:1px solid #d97706;color:#fff">C</div>
    isCandidateUCC = true
  </div>
  <div class="li">
    <div class="lsq" style="background:#fff;border:1px solid #ccc"></div>
    isCandidateUCC = false
  </div>
  <div class="li">
    <div class="lsq" style="background:#4ade80;border:1px solid #16a34a;color:#fff">V</div>
    isValidatedUCC = true
  </div>
  <div class="li">
    <div class="lsq" style="background:#fff;border:1px solid #ccc"></div>
    isValidatedUCC = false
  </div>
  <span style="margin-left:10px;color:#888">Node:</span>
  <div class="li"><div class="lsq" style="background:#eff6ff;border:1px solid #93c5fd"></div> default</div>
  <div class="li"><div class="lsq" style="background:#fffbeb;border:1px solid #fbbf24"></div> candidate</div>
  <div class="li"><div class="lsq" style="background:#f0fdf4;border:1px solid #4ade80"></div> validated</div>
</div>

<div class="canvas-wrap" id="wrap">
  <div id="empty" style="display:none">
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
const wrap    = document.getElementById('wrap');
const svgEl   = document.getElementById('svg');
const fileInfo = document.getElementById('file-info');
const emptyEl = document.getElementById('empty');

let scale=1, tx=0, ty=0;
let dragging=false, sx=0, sy=0, stx=0, sty=0;

function apply() {{
  svgEl.style.transform = `translate(${{tx}}px,${{ty}}px) scale(${{scale}})`;
}}

function fitToWindow(w, h) {{
  const ww=wrap.clientWidth, wh=wrap.clientHeight;
  scale = Math.min(ww/w, wh/h, 1);
  tx = (ww - w*scale)/2;
  ty = 20;
  apply();
}}

window.addEventListener('load', () => fitToWindow({SVG_W}, {SVG_H}));

document.getElementById('file-input').addEventListener('change', e => {{
  const file = e.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = ev => {{
    try {{
      const data = JSON.parse(ev.target.result);
      loadTree(data, file.name);
    }} catch(err) {{ alert('Invalid JSON: ' + err.message); }}
  }};
  reader.readAsText(file);
  e.target.value = '';
}});

function loadTree(data, filename) {{
  const r = buildSVG(data);
  svgEl.setAttribute('viewBox', `0 0 ${{r.w}} ${{r.h}}`);
  svgEl.setAttribute('width',  r.w);
  svgEl.setAttribute('height', r.h);
  svgEl.innerHTML = r.inner;
  emptyEl.style.display = 'none';
  svgEl.style.display = 'block';
  fileInfo.textContent = `${{r.nodeCount}} nodes · ${{filename}}`;
  fitToWindow(r.w, r.h);
}}

// JS layout + SVG builder
function parseNode(obj, parentLhs) {{
  parentLhs = parentLhs || [];
  const nodeVal = obj.node !== undefined ? obj.node : 'root';
  const lhs = nodeVal !== 'root' ? [...parentLhs, nodeVal] : [...parentLhs];
  const id  = lhs.length ? 'node_'+lhs.join('_') : 'node_root';
  return {{
    id, node: nodeVal, lhs,
    cand:  !!obj.cand,
    valid: !!obj.valid,
    children: (obj.children||[]).map(c => parseNode(c, lhs))
  }};
}}

function computeLayout(node, depth, counter) {{
  depth = depth||0;
  if (!node.children.length) {{ node._x=counter[0]++; node._y=depth; return 1; }}
  let total=0;
  for (const c of node.children) total += computeLayout(c, depth+1, counter);
  node._x = (node.children[0]._x + node.children[node.children.length-1]._x)/2;
  node._y = depth;
  return total;
}}

function collectNodes(node, arr) {{
  arr=arr||[]; arr.push(node);
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

  const maxDepth = Math.max(...all.map(n=>n._y));
  const maxX     = Math.max(...all.map(n=>n._x));

  const SQ=16, SQ_GAP=5, PAIR_W=SQ*2+SQ_GAP;
  const NODE_R=16, ABOVE_GAP=6;
  const TOTAL_ABOVE = SQ+ABOVE_GAP+NODE_R;
  const H_SPACING = Math.max(PAIR_W+40, 90);
  const V_SPACING = TOTAL_ABOVE+NODE_R+30;
  const LEFT_PAD  = Math.max(60, Math.floor(PAIR_W/2)+24);
  const TOP_PAD   = 50;
  const SVG_W = Math.max(500, Math.ceil((maxX+1)*H_SPACING)+LEFT_PAD*2);
  const SVG_H = Math.ceil((maxDepth+1)*V_SPACING)+TOP_PAD+40;

  function px(n) {{
    return [LEFT_PAD + n._x*H_SPACING, TOP_PAD + n._y*V_SPACING + TOTAL_ABOVE];
  }}

  let out='';
  const KEYS=[['cand','C'],['valid','V']];

  for (const n of all) {{
    const [x,y]=px(n);
    for (const child of n.children) {{
      const [cx,cy]=px(child);
      out+=`<line x1="${{x.toFixed(1)}}" y1="${{(y+NODE_R).toFixed(1)}}" x2="${{cx.toFixed(1)}}" y2="${{(cy-TOTAL_ABOVE).toFixed(1)}}" class="edge"/>`;
    }}

    const pairLeft = x - PAIR_W/2;
    const sqY = y - TOTAL_ABOVE;

    KEYS.forEach(([key, label], i) => {{
      const sx2 = pairLeft + i*(SQ+SQ_GAP);
      const isSet = n[key];
      out += `<rect x="${{sx2.toFixed(1)}}" y="${{sqY.toFixed(1)}}" width="${{SQ}}" height="${{SQ}}" rx="3" class="sq ${{isSet?'sq-'+key:'sq-off'}}"/>`;
      if (isSet) {{
        out += `<text x="${{(sx2+SQ/2).toFixed(1)}}" y="${{(sqY+SQ/2).toFixed(1)}}" dominant-baseline="central" text-anchor="middle" class="sq-lbl sq-lbl-set">${{label}}</text>`;
      }}
    }});

    const cls = n.valid ? 'nc-valid' : n.cand ? 'nc-cand' : n.node==='root' ? 'nc-root' : 'nc-def';
    const label = n.node==='root' ? 'R' : String(n.node);
    const lhs   = n.lhs.length ? '{{'+n.lhs.join(',')+'}}' : '∅';

    out += `<circle cx="${{x.toFixed(1)}}" cy="${{y.toFixed(1)}}" r="${{NODE_R}}" class="${{cls}}"/>`;
    out += `<text x="${{x.toFixed(1)}}" y="${{y.toFixed(1)}}" dominant-baseline="central" text-anchor="middle" class="nl">${{esc(label)}}</text>`;
    out += `<text x="${{x.toFixed(1)}}" y="${{(y+NODE_R+10).toFixed(1)}}" dominant-baseline="hanging" text-anchor="middle" class="ll">${{esc(lhs)}}</text>`;
  }}

  return {{ w:SVG_W, h:SVG_H, inner:out, nodeCount:all.length }};
}}

// Pan / zoom
wrap.addEventListener('wheel', e => {{
  e.preventDefault();
  const rect=wrap.getBoundingClientRect();
  const mx=e.clientX-rect.left, my=e.clientY-rect.top;
  const delta=e.deltaY<0?1.12:1/1.12;
  const ns=Math.min(Math.max(scale*delta,0.08),10);
  tx=mx-(mx-tx)*(ns/scale); ty=my-(my-ty)*(ns/scale); scale=ns; apply();
}},{{passive:false}});

wrap.addEventListener('mousedown', e=>{{ dragging=true; sx=e.clientX; sy=e.clientY; stx=tx; sty=ty; wrap.style.cursor='grabbing'; }});
window.addEventListener('mousemove', e=>{{ if(!dragging)return; tx=stx+(e.clientX-sx); ty=sty+(e.clientY-sy); apply(); }});
window.addEventListener('mouseup', ()=>{{ dragging=false; wrap.style.cursor='grab'; }});

let lastDist=null;
wrap.addEventListener('touchstart', e=>{{
  if(e.touches.length===1){{ sx=e.touches[0].clientX; sy=e.touches[0].clientY; stx=tx; sty=ty; }}
  else if(e.touches.length===2) lastDist=Math.hypot(e.touches[0].clientX-e.touches[1].clientX,e.touches[0].clientY-e.touches[1].clientY);
}},{{passive:true}});
wrap.addEventListener('touchmove', e=>{{
  e.preventDefault();
  if(e.touches.length===1){{ tx=stx+(e.touches[0].clientX-sx); ty=sty+(e.touches[0].clientY-sy); apply(); }}
  else if(e.touches.length===2){{
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
    print(f"✓ {output_path}  ({node_count} nodes)")
    print(f"  Open: file://{os.path.abspath(output_path)}")


def main():
    if len(sys.argv) < 2:
        output_path = "../backup/visualizers/ucc_tree_visualize.html"
        root = parse_node({"node": "root", "cand": False, "valid": False, "children": []})
        render_html(root, output_path)
        return
    input_path  = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else os.path.splitext(input_path)[0] + ".html"
    with open(input_path, "r", encoding="utf-8") as f:
        data = json.load(f)
    root = parse_node(data)
    render_html(root, output_path)

if __name__ == "__main__":
    main()