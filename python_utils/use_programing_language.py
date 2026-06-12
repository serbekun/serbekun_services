import subprocess
import json

def get_percent(sum, pease):
    return (pease / sum) * 100

result = subprocess.run(["cloc", "src/", "python_utils/", "sh_utils/", "--json"], capture_output=True, text=True)
result = json.loads(result.stdout)

sum = result["SUM"]["code"]
java = result["Java"]["code"]
html = result["HTML"]["code"]
css = result["CSS"]["code"]
js = result["JavaScript"]["code"]
py = result["Python"]["code"]

java_percent = get_percent(sum, java)
html_percent = get_percent(sum, html)
css_percent = get_percent(sum, css)
js_percent = get_percent(sum, js)
py_percent = get_percent(sum, py)

print(f"Java: {java_percent}%")
print(f"Css : {css_percent}%")
print(f"Js  : {js_percent}%")
print(f"Html: {html_percent}%")
print(f"Py  : {py_percent}%")

