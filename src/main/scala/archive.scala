package com.hhalex.jb

import java.io.{ByteArrayOutputStream, File}
import java.nio.file.Path

import cats.Functor
import fs2.{Chunk, Pipe, Stream}
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import cats.effect.{Blocker, ContextShift, IO}
import fs2.io.file

object archive {

  def streamFiles(bufferSize: Int, b: Blocker)(implicit ctx: ContextShift[IO]): Pipe[IO, Path, (String, Stream[IO, Byte])] = _.flatMap(
    path =>
      Stream.emit((path.toString, file.readAll[IO](path, b, bufferSize)))
  )

  def tar(bufferSize: Int): Pipe[IO, (String, Stream[IO, Byte]), Byte] =
    in =>
      Stream.suspend {
        val bos = new ByteArrayOutputStream(bufferSize)
        val taros = new TarArchiveOutputStream(bos, bufferSize)
        def slurpBytes = {
          val back = bos.toByteArray
          bos.reset()
          Stream.chunk(Chunk.bytes(back))
        }

        val body = in.flatMap {
          case (path, fileStream) =>
            val fileData = fileStream.compile.toList.unsafeRunSync().toArray
            Stream.suspend {
              val entry = new TarArchiveEntry(path)
              entry.setSize(fileData.length)
              taros.putArchiveEntry(entry)
              taros.write(fileData)
              taros.closeArchiveEntry()
              taros.flush()
              slurpBytes
            }
        }

        val trailer = Stream.suspend {
          taros.finish()
          taros.close()
          slurpBytes
        }

        body ++ trailer
      }

  def untar[F[_]](bufferSize: Int): Pipe[F, Byte, File] = ???
}
