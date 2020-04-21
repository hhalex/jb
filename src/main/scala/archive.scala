package com.hhalex.jb

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.file.Path

import fs2.{Chunk, Pipe, Stream}
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream, TarArchiveOutputStream}
import cats.effect.{Blocker, ContextShift, IO}
import fs2.Chunk.Bytes
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

  def untar(bufferSize: Int)(implicit ctx: ContextShift[IO]): Pipe[IO, Byte, (String, Stream[IO, Byte])] =
    in => in.through(fs2.io.toInputStream).flatMap {
      bos => {
        val taros = new TarArchiveInputStream(bos, bufferSize)

        def createStream(s: TarArchiveInputStream): Stream[IO, (TarArchiveEntry, TarArchiveInputStream)] = Stream.suspend {
          Option(s.getNextTarEntry) match {
            case None => Stream.empty
            case Some(current) if current.isFile => Stream.emit((current, s)) ++ createStream(s)
            case _ => createStream(s)
          }
        }

        createStream(taros).flatMap({
          case (entry, s) => {
            val name = entry.getName
            val buffer = new Array[Byte](bufferSize)
            def file: Stream[IO, Byte] = Stream.suspend {
              val nbReadBytes = s.read(buffer)
              if (nbReadBytes != -1)
                Stream.chunk(Bytes(buffer, 0, nbReadBytes)) ++ file
              else Stream.empty
            }
            Stream.emit((name, file))
          }
        })
      }
    }
}
