package com.javacppstream.livedrmpackage;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.avcodec.AVBitStreamFilter;
import org.bytedeco.javacpp.avcodec.AVPacket;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;

public class StreamEngine {
    String inputPath;
    String outputRtspUri;

    public StreamEngine() {
        this.inputPath = "C:/xampp/htdocs/javacpptest/out.flv";
        this.outputRtspUri = "udp://@localhost:1002";
    }

    public void run() {
        int videoStreamIndex = -1;
        int audioStreamIndex = -1;
        int ret = -1;

        AVFormatContext ictx = new AVFormatContext(null);
        AVOutputFormat ofmt = null;
        AVDictionary inOptions = new AVDictionary(null);
        AVInputFormat ifmt = null;

        avformat_network_init();
        av_log_set_level(AV_LOG_DEBUG);
        av_dict_set(inOptions, "stream_loop", "99", 0);
        av_dict_set(inOptions, "re", "", 0);
        // av_dict_set(inOptions, "analyzeduration", "1G", 0);
        // av_dict_set(inOptions, "probesize", "50M", 0);
        if ((ret = avformat_open_input(ictx, inputPath, ifmt, inOptions)) < 0) {
            System.out.println("Could not open input file");
            return;
        }

        if ((ret = avformat_find_stream_info(ictx, inOptions)) < 0) {
            System.out.println("Failed to retrieve input stream information");
            return;
        }
        av_dump_format(ictx, 0, inputPath, 0);

        AVFormatContext octx = new AVFormatContext(null);

        if ((ret = avformat_alloc_output_context2(octx, null, "mpegts", outputRtspUri)) < 0) {
            System.out.println("Could not create output context");
            return;
        }

        ofmt = octx.oformat();

        for (int i = 0; i < ictx.nb_streams(); i++) {
            AVStream inStream = ictx.streams(i);
            AVStream outStream = avformat_new_stream(octx, inStream.codec().codec());

            if (outStream == null) {
                System.out.println("Failed allocating output stream");
                return;
            }

            if ((ret = avcodec_parameters_copy(outStream.codecpar(), inStream.codecpar())) < 0) {
                System.out.println("Failed to copy codec parameters");
                return;
            }

            outStream.codecpar().codec_tag(0);
            outStream.codec().codec_tag(0);
            if ((octx.oformat().flags() & AVFMT_GLOBALHEADER) != 1) {
                outStream.codec().flags(outStream.codec().flags() | 0);
            }
        }

        for (int i = 0; i < ictx.nb_streams(); i++) {
            if (ictx.streams(i).codec().codec_type() == AVMEDIA_TYPE_VIDEO) {
                videoStreamIndex = i;
                break;
            }
        }

        for (int i = 0; i < ictx.nb_streams(); i++) {
            if (ictx.streams(i).codec().codec_type() == AVMEDIA_TYPE_AUDIO) {
                audioStreamIndex = i;
                break;
            }
        }

        av_dump_format(octx, 0, outputRtspUri, 1);

        if ((ofmt.flags() & AVFMT_NOFILE) == 0) {
            AVIOContext pb = new AVIOContext(null);
            if ((ret = avio_open(pb, outputRtspUri, AVIO_FLAG_WRITE)) < 0) {
                System.out.println("Could not open output file");
                return;
            }
            octx.pb(pb);
        }
        AVDictionary avOutDict = new AVDictionary(null);
        av_dict_set(avOutDict, "codec", "copy", 0);
        // av_dict_set(avOutDict,"map","0",0);
        // av_dict_set(avOutDict, "c:v", "libx264", 0);
        // av_dict_set(avOutDict, "c:a", "aac", 0);
        av_dict_set(avOutDict, "b:v", "3M", 0);
        av_dict_set(avOutDict, "g", "25", 0);
        av_dict_set(avOutDict, "keyint_min", "25", 0);
        av_dict_set(avOutDict, "maxrate", "3M", 0);
        av_dict_set(avOutDict, "bufsize", "3M", 0);
        av_dict_set(avOutDict, "f", "mpegts", 0);

        ret = avformat_write_header(octx, avOutDict);
        if (ret < 0) {
            throw new RuntimeException("avformat_write_header error!");
        }

        AVPacket pkt = new AVPacket();
        while (true) {
            ret = av_read_frame(ictx, pkt);
            if (ret < 0) {
                break;
            }

            ret = av_interleaved_write_frame(octx, pkt);
            if (ret < 0) {
                System.out.println("Error packet write");
                break;
            }
            av_packet_unref(pkt);

        }

    }
}
