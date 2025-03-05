package jcraft.jorbis;

import jcraft.jogg.Packet;
import jcraft.jogg.Page;
import jcraft.jogg.StreamState;
import jcraft.jogg.SyncState;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

/**
 * Decode an OGG file to PCM data. This class is based on the example
 * code that accompanies the Java OGG libraries (hence the lack of detailed)
 * explanation.
 *
 * @author Kevin Glass
 */
public class OggVorbisDecoder {

  public byte[] pcmSampleData;

  public int sampleRate;

  public int loopStart;
  public int loopEnd;

  /** The conversion buffer size */
  private int convsize = 1024 * 2;
  /** The buffer used to read OGG file */
  private final byte[] convbuffer = new byte[convsize]; // take 8k out of the data segment, not the stack

  /**
   * Create a new OGG decoder
   * @param input
   */
  public OggVorbisDecoder(InputStream input) {
    try {
      getData(input);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the data out of an OGG file
   *
   * @param input The input stream from which to read the OGG file
   */
  public void getData(InputStream input) throws IOException {
    // the following code come from an example in the Java OGG library.
    // Its extremely complicated and a good example of a library
    // that is potentially to low level for average users. I'd suggest
    // accepting this code as working and not thinking too hard
    // on what its actually doing

    ByteArrayOutputStream dataout = new ByteArrayOutputStream();

    SyncState oy = new SyncState(); // sync and verify incoming physical bitstream
    StreamState os = new StreamState(); // take physical pages, weld into a logical stream of packets
    Page og = new Page(); // one Ogg bitstream page.  Vorbis packets are inside
    Packet op = new Packet(); // one raw packet of data for decode

    Info vi = new Info(); // struct that stores all the static vorbis bitstream settings
    Comment vc = new Comment(); // struct that stores all the bitstream user comments
    DspState vd = new DspState(); // central working state for the packet->PCM decoder
    Block vb = new Block(vd); // local working space for packet->PCM decode

    byte[] buffer;
    int bytes = 0;

    boolean bigEndian = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
    // Decode setup

    oy.init(); // Now we can read pages

    while (true) { // we repeat if the bitstream is chained
      int eos = 0;

      // grab some data at the head of the stream.  We want the first page
      // (which is guaranteed to be small and only contain the Vorbis
      // stream initial header) We need the first page to get the stream
      // serialno.

      // submit a 4k block to libvorbis' Ogg layer
      int index = oy.buffer(1024);
      buffer = oy.data;
      try {
        bytes = input.read(buffer, index, 1024);
      } catch (Exception e) {
        throw new IOException(e.getMessage());
      }
      oy.wrote(bytes);

      // Get the first page.
      if (oy.pageout(og) != 1) {
        // have we simply run out of data?  If so, we're done.
        if (bytes < 1024)
          break;

        // error case.  Must not be Vorbis data
        throw new IOException("Input does not appear to be an Ogg bitstream.");
      }

      // Get the serial number and set up the rest of decode.
      // serialno first; use it to set up a logical stream
      os.init(og.serialno());

      // extract the initial header from the first page and verify that the
      // Ogg bitstream is in fact Vorbis data

      // I handle the initial header first instead of just having the code
      // read all three Vorbis headers at once because reading the initial
      // header is an easy way to identify a Vorbis bitstream and it's
      // useful to see that functionality seperated out.

      vi.init();
      vc.init();
      if (os.pagein(og) < 0) {
        // error; stream version mismatch perhaps
        throw new IOException("Error reading first page of Ogg bitstream data.");
      }

      if (os.packetout(op) != 1) {
        // no page? must not be vorbis
        throw new IOException("Error reading initial header packet.");
      }

      if (vi.synthesis_headerin(vc, op) < 0) {
        // error case; not a vorbis header
        throw new IOException("This Ogg bitstream does not contain Vorbis audio data.");
      }

      // At this point, we're sure we're Vorbis.  We've set up the logical
      // (Ogg) bitstream decoder.  Get the comment and codebook headers and
      // set up the Vorbis decoder

      // The next two packets in order are the comment and codebook headers.
      // They're likely large and may span multiple pages.  Thus we reead
      // and submit data until we get our two pacakets, watching that no
      // pages are missing.  If a page is missing, error out; losing a
      // header page is the only place where missing data is fatal. */

      int i = 0;
      while (i < 2) {
        while (i < 2) {

          int result = oy.pageout(og);
          if (result == 0)
            break; // Need more data
          // Don't complain about missing or corrupt data yet.  We'll
          // catch it at the packet output phase

          if (result == 1) {
            os.pagein(og); // we can ignore any errors here
            // as they'll also become apparent
            // at packetout
            while (i < 2) {
              result = os.packetout(op);
              if (result == 0)
                break;
              if (result == -1) {
                // Uh oh; data at some point was corrupted or missing!
                // We can't tolerate that in a header.  Die.
                throw new IOException("Corrupt secondary header.  Exiting.");
              }
              vi.synthesis_headerin(vc, op);
              i++;
            }
          }
        }
        // no harm in not checking before adding more
        index = oy.buffer(1024);
        buffer = oy.data;
        try {
          bytes = input.read(buffer, index, 1024);
        } catch (Exception e) {
          throw new IOException(e.getMessage());
        }
        if (bytes == 0 && i < 2) {
          throw new IOException("End of file before finding all Vorbis headers!");
        }
        oy.wrote(bytes);
      }

      for (int commentIndex = 0; commentIndex < vc.comments; commentIndex++) {
        String comment = vc.getComment(commentIndex);
        if (comment.toLowerCase().contains("loop start=")) {
          loopStart = Integer.parseInt(comment.toLowerCase().replace("loop start=", "").trim());
        }
        if (comment.toLowerCase().contains("loop end=")) {
          loopEnd = Integer.parseInt(comment.toLowerCase().replace("loop end=", "").trim());
        }
        if (comment.toLowerCase().contains("sample size=")) {
          int sampleSize = Integer.parseInt(comment.toLowerCase().replace("sample size=", "").trim());
          if (loopEnd != sampleSize) {
            loopEnd = sampleSize;
          }
        }
        if (comment.toLowerCase().contains("sample rate=")) {
          sampleRate = Integer.parseInt(comment.toLowerCase().replace("sample rate=", "").trim());
        }
      }

      convsize = 1024 / vi.channels;

      // OK, got and parsed all three headers. Initialize the Vorbis
      //  packet->PCM decoder.
      vd.synthesis_init(vi); // central decode state
      vb.init(vd); // local state for most of the decode
      // so multiple block decodes can
      // proceed in parallel.  We could init
      // multiple vorbis_block structures
      // for vd here

      float[][][] _pcm = new float[1][][];
      int[] _index = new int[vi.channels];
      // The rest is just a straight decode loop until end of stream
      while (eos == 0) {
        while (eos == 0) {

          int result = oy.pageout(og);
          if (result == 0)
            break; // need more data
          if (result == -1) { // missing or corrupt data at this page position
            System.err.println("Corrupt or missing data in bitstream; continuing...");
          } else {
            os.pagein(og); // can safely ignore errors at
            // this point
            while (true) {
              result = os.packetout(op);

              if (result == 0)
                break; // need more data
              if (result == -1) { // missing or corrupt data at this page position
                // no reason to complain; already complained above
              } else {
                // we have a packet.  Decode it
                int samples;
                if (vb.synthesis(op) == 0) { // test for success!
                  vd.synthesis_blockin(vb);
                }

                // **pcm is a multichannel float vector.  In stereo, for
                // example, pcm[0] is left, and pcm[1] is right.  samples is
                // the size of each channel.  Convert the float values
                // (-1.<=range<=1.) to whatever PCM format and write it out

                while ((samples = vd.synthesis_pcmout(_pcm,
                        _index)) > 0) {
                  float[][] pcm = _pcm[0];
                  int bout = (Math.min(samples, convsize));

                  // convert floats to 16 bit signed ints (host order) and
                  // interleave
                  for (i = 0; i < vi.channels; i++) {
                    int ptr = i * 2;
                    //int ptr=i;
                    int mono = _index[i];
                    for (int j = 0; j < bout; j++) {
                      int val = (int) (pcm[i][mono + j] * 32767.);
                      //                            short val=(short)(pcm[i][mono+j]*32767.);
                      //                            int val=(int)Math.round(pcm[i][mono+j]*32767.);
                      // might as well guard against clipping
                      if (val > 32767) {
                        val = 32767;
                      }
                      if (val < -32768) {
                        val = -32768;
                      }
                      if (val < 0)
                        val = val | 0x8000;

                      if (bigEndian) {
                        convbuffer[ptr] = (byte) (val >>> 8);
                        convbuffer[ptr + 1] = (byte) (val);
                      } else {
                        convbuffer[ptr] = (byte) (val);
                        convbuffer[ptr + 1] = (byte) (val >>> 8);
                      }
                      ptr += 2 * (vi.channels);
                    }
                  }

                  dataout.write(convbuffer, 0, 2 * vi.channels * bout);

                  vd.synthesis_read(bout); // tell libvorbis how
                  // many samples we
                  // actually consumed
                }
              }
            }
            if (og.eos() != 0)
              eos = 1;
          }
        }
        if (eos == 0) {
          index = oy.buffer(1024);
          buffer = oy.data;
          try {
            bytes = input.read(buffer, index, 1024);
          } catch (Exception e) {
            throw new IOException();
          }
          oy.wrote(bytes);
          if (bytes == 0)
            eos = 1;
        }
      }

      // clean up this logical bitstream; before exit we see if we're
      // followed by another [chained]

      os.clear();

      // ogg_page and ogg_packet structs always point to storage in
      // libvorbis.  They're never freed or manipulated directly

      vb.clear();
      vd.clear();
      vi.clear(); // must be called last
    }

    // OK, clean up the framer
    oy.clear();

    pcmSampleData = dataout.toByteArray();
  }
}