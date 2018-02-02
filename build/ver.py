import subprocess
import sys
import re

def extractVersion(tag):
    ver = "0.0.0"

    m = re.match("v([0-9]+\.[0-9]+)\.([0-9]+)-([0-9]+)-g(.+)", tag)
    if m:
        return m.group(1) + "." + str(int(m.group(2)) + int(m.group(3)))

    m = re.match("v([0-9]+\.[0-9]+)-([0-9]+)-g(.+)", tag)
    if m:
        return m.group(1) + "." + m.group(2)

    m = re.match("v([0-9]+\.[0-9]+\.[0-9]+)", tag)
    if m:
        return m.group(1)

    m = re.match("v([0-9]+\.[0-9]+)", tag)
    if m:
        return m.group(1) + ".0"

    return "0.0.0"

if __name__ == '__main__':
    p = subprocess.Popen(["git", "describe", "--match", "v*"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    stdoutdata, stderrdata = p.communicate()

    if p.returncode != 0:
        sys.stderr.write(stderrdata)
        exit(1)

    tag = stdoutdata.decode(sys.getdefaultencoding()).strip()

    print(extractVersion(tag))

    exit(0)
