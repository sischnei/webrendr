package net.tridex.endpoints;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/renderer")
public class RendererEndpoint {

    private final static Logger LOGGER = LoggerFactory.getLogger(RendererEndpoint.class);


    private final String phantomJS;
    private final File scriptPath;

    @Autowired
    public RendererEndpoint(@Value("${webrendr.phantomjs}") final String phantomJS) throws IOException {
        this.phantomJS = phantomJS;

        scriptPath = File.createTempFile("rasterize", ".js");

        try (FileWriter writer = new FileWriter(scriptPath)) {
            IOUtils.copy(RendererEndpoint.class.getResourceAsStream("/scripts/rasterize.js"), writer);
        }

    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<byte[]> getRendering(@RequestParam("url") final String url, @RequestParam(value = "selector", defaultValue = "html") final String selector) throws IOException {
        File outputFile = File.createTempFile("output", ".png");
        LOGGER.info("Writing output of url [{}] to path [{}]", url, outputFile.getAbsolutePath());
        ProcessBuilder processBuilder = new ProcessBuilder(phantomJS, scriptPath.getAbsolutePath(), url, outputFile.getAbsolutePath(), selector);
        Process process = processBuilder.start();
        try {
            process.waitFor(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            process.destroy();
            LOGGER.error("Failed request to url [{}]", url);
            return new ResponseEntity<byte[]>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        byte[] image = Files.readAllBytes(outputFile.toPath());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(image.length);
        return new ResponseEntity<>(image, headers, HttpStatus.OK);
    }
}
