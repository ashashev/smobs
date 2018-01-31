/*
 * smobs
 * A tool for simple migration of bitbucket server repositories to another bitbucket server
 * Copyright (C) 2018, Aleksei Shashev
 * Licensed under GPLv3 license (see LICENSE)
 */

package com.github.ashashev.smobs

import java.io.IOException
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

package object application {
  def collored(color: String)(f : => Unit) = {
    print(s"${Console.RESET}${color}")
    f
    print(s"${Console.RESET}")
  }

  val YELLOW = collored(Console.YELLOW)(_)
  val RED = collored(Console.RED)(_)

  def deleteDirectory(spath: String): Unit = {
    Files.walkFileTree(Paths.get(spath), new FileVisitor[Path] {
      @throws[IOException]
      override def preVisitDirectory(dir: Path, attr: BasicFileAttributes): FileVisitResult = {
        FileVisitResult.CONTINUE
      }

      @throws[IOException]
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        if (file != null) {
          Files.delete(file)
        }
        FileVisitResult.CONTINUE
      }

      @throws[IOException]
      def visitFileFailed(file: Path, exc: IOException): FileVisitResult = {
        FileVisitResult.CONTINUE
      }

      @throws[IOException]
      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
        if (dir != null) {
          Files.delete(dir)
        }
        FileVisitResult.CONTINUE
      }
    })
  }
}
