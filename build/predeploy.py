import sys
import os
import shutil

if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.stderr.write("Not enought arguments")
        exit(1)

    workdir = sys.argv[1]
    ver = sys.argv[2]

    indir = os.path.join(workdir, "target", "scala-2.12")
    outdir = os.path.join(workdir, "output")

    inJarname = "smobs-assembly-" + ver + ".jar"
    outJarname = "smobs.jar"

    srcJarname = os.path.join(indir, inJarname)
    dstJarname = os.path.join(outdir, outJarname)

    shutil.copy(srcJarname, dstJarname)