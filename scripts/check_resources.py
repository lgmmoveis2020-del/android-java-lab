#!/usr/bin/env python3
"""Verificações estáticas dos recursos do módulo :app (roda sem o Android SDK).

- Todo recurso referenciado (R.xxx.yyy ou @xxx/yyy) existe.
- Chaves de values/strings.xml e values-en/strings.xml são idênticas
  (lint: MissingTranslation = erro, ExtraTranslation = fatal).
- Todo `binding.<id>` usado em Java existe no layout correspondente.
- Todos os XMLs de recursos são bem formados.
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main")
RES = os.path.join(ROOT, "res")
JAVA = os.path.join(ROOT, "java")
errors = []

def res_files(prefix):
    for d in sorted(os.listdir(RES)):
        if d == prefix or d.startswith(prefix + "-"):
            for f in sorted(os.listdir(os.path.join(RES, d))):
                yield d, os.path.join(RES, d, f)

# 1. XML well-formedness + declared resources
declared = {k: set() for k in ("string", "color", "drawable", "dimen", "integer", "plurals",
                               "menu", "layout", "mipmap", "xml", "style", "array", "id")}
strings_by_qualifier = {}
for d in sorted(os.listdir(RES)):
    for f in sorted(os.listdir(os.path.join(RES, d))):
        path = os.path.join(RES, d, f)
        try:
            tree = ET.parse(path)
        except ET.ParseError as e:
            errors.append(f"XML inválido: {path}: {e}")
            continue
        name = os.path.splitext(f)[0]
        kind = d.split("-")[0]
        if kind in ("drawable", "layout", "menu", "mipmap", "xml"):
            declared[kind].add(name)
        if kind == "values":
            for el in tree.getroot():
                tag = el.tag
                nm = el.get("name")
                if tag == "item" and el.get("type"):
                    tag = el.get("type")
                if tag in declared and nm:
                    declared[tag].add(nm)
                if tag == "string":
                    strings_by_qualifier.setdefault(d, {})[nm] = el.get("translatable", "true") != "false"
        text = open(path, encoding="utf-8").read()
        for m in re.finditer(r"@\+id/(\w+)", text):
            declared["id"].add(m.group(1))

# 2. referenced resources
referenced = {k: set() for k in declared}
for root, _, files in os.walk(JAVA):
    for f in files:
        if f.endswith(".java"):
            s = open(os.path.join(root, f), encoding="utf-8").read()
            s = re.sub(r"android\.R\.\w+\.\w+", "", s)
            s = re.sub(r"androidx\.\w+(\.\w+)*\.R\.\w+\.\w+", "", s)
            s = re.sub(r"com\.google\.android\.material\.R\.\w+\.\w+", "", s)
            for m in re.finditer(r"\bR\.(\w+)\.(\w+)", s):
                if m.group(1) in referenced:
                    referenced[m.group(1)].add(m.group(2))
for d in os.listdir(RES):
    for f in os.listdir(os.path.join(RES, d)):
        s = open(os.path.join(RES, d, f), encoding="utf-8").read()
        for m in re.finditer(r"@(\w+)/(\w+)", s):
            if m.group(1) in referenced:
                referenced[m.group(1)].add(m.group(2))
manifest = open(os.path.join(ROOT, "AndroidManifest.xml"), encoding="utf-8").read()
for m in re.finditer(r"@(\w+)/(\w+)", manifest):
    if m.group(1) in referenced:
        referenced[m.group(1)].add(m.group(2))

for kind, names in referenced.items():
    if kind == "style":
        continue  # estilos de bibliotecas (Widget.Material3.*) não são declarados aqui
    for n in sorted(names - declared[kind]):
        errors.append(f"Recurso referenciado mas não declarado: {kind}/{n}")

# 3. strings parity
base = strings_by_qualifier.get("values", {})
translatable = {k for k, t in base.items() if t}
for qualifier, strings in strings_by_qualifier.items():
    if qualifier == "values":
        continue
    keys = set(strings)
    for k in sorted(translatable - keys):
        errors.append(f"{qualifier}: falta tradução de '{k}' (MissingTranslation)")
    for k in sorted(keys - set(base)):
        errors.append(f"{qualifier}: '{k}' não existe em values/ (ExtraTranslation)")
    for k in sorted(keys & set(base)):
        if not base[k]:
            errors.append(f"{qualifier}: '{k}' é translatable=false e não deve ser traduzido")

# 4. format argument parity between locales
def fmt_args(s):
    return sorted(re.findall(r"%(\d+)\$[sdfe]", s or ""))
texts = {}
for d, path in res_files("values"):
    if os.path.basename(path) != "strings.xml":
        continue
    for el in ET.parse(path).getroot():
        if el.tag == "string":
            texts.setdefault(el.get("name"), {})[d] = "".join(el.itertext())
for name, by_q in texts.items():
    base_args = fmt_args(by_q.get("values"))
    for q, t in by_q.items():
        if fmt_args(t) != base_args:
            errors.append(f"string '{name}': argumentos de formato diferem entre values e {q}")

# 5. binding ids vs layouts
def camel(snake):
    parts = snake.split("_")
    return parts[0] + "".join(p[:1].upper() + p[1:] for p in parts[1:])
layout_ids = {}
for d, path in res_files("layout"):
    name = os.path.splitext(os.path.basename(path))[0]
    layout_ids.setdefault(name, set()).update(
        camel(m) for m in re.findall(r"@\+id/(\w+)", open(path, encoding="utf-8").read()))
for root, _, files in os.walk(JAVA):
    for f in files:
        if not f.endswith(".java"):
            continue
        s = open(os.path.join(root, f), encoding="utf-8").read()
        # binding variables: "XxxBinding name = XxxBinding.inflate" ou campos
        bindings = {}
        for m in re.finditer(r"\b([A-Z]\w+)Binding\s+(\w+)\s*(?:=|;)", s):
            cls, var = m.group(1), m.group(2)
            layout = re.sub(r"(?<!^)([A-Z])", r"_\1", cls).lower()
            bindings[var] = layout
        for var, layout in bindings.items():
            ids = layout_ids.get(layout)
            if ids is None:
                errors.append(f"{f}: layout '{layout}.xml' para {var} não existe")
                continue
            for m in re.finditer(r"(?<![.\w])" + re.escape(var) + r"\.(\w+)", s):
                member = m.group(1)
                if member in ("getRoot", "inflate", "bind"):
                    continue
                if member not in ids:
                    errors.append(f"{f}: {var}.{member} não existe em {layout}.xml")

# 6. manifest activities exist
pkg = "com.lgm.androidlab"
for m in re.finditer(r'android:name="(\.[\w.]+)"', manifest):
    cls = pkg + m.group(1)
    path = os.path.join(JAVA, *cls.split(".")) + ".java"
    if not os.path.exists(path):
        errors.append(f"Manifest referencia classe inexistente: {cls}")

if errors:
    print("\n".join(errors))
    print(f"\n{len(errors)} problema(s).")
    sys.exit(1)
print(f"OK: {sum(len(v) for v in declared.values())} recursos declarados, "
      f"{len(base)} strings, {len(layout_ids)} layouts, sem inconsistências.")
