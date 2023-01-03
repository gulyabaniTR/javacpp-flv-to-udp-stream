package com.javacppstream.livedrmpackage;


import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stream")
@CrossOrigin(origins = "*")
public class StreamService {

        @RequestMapping("/start")
        public String startStream() {
            StreamEngine streamEngine = new StreamEngine();
            streamEngine.run();
            return "Stream started";
        }
}
