import zipfile
import sys

with open("d:\\Order Analyzer\\check_out.txt", "w", encoding="utf-8") as out:
    try:
        with zipfile.ZipFile("d:\\Order Analyzer\\1773752191.csv", 'r') as z:
            out.write("ZIP contents: " + str(z.namelist()[:10]))
    except zipfile.BadZipFile:
        out.write("Not a zip file. Reading as text...\n")
        with open("d:\\Order Analyzer\\1773752191.csv", 'r', encoding='utf-8', errors='replace') as f:
            out.write(f.read(1000))
