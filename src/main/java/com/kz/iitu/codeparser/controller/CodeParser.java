package com.kz.iitu.codeparser.controller;

import com.kz.iitu.codeparser.model.ProjectResult;
import com.kz.iitu.codeparser.service.CodeParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/rest")
@CrossOrigin(allowedHeaders = "*", origins = "*")
public class CodeParser {

    private static final Logger logger = LoggerFactory.getLogger(CodeParser.class);

    @Autowired
    CodeParserService codeParserService;

    @PostMapping("/zip")
    public ProjectResult checkCodeResult(HttpServletRequest servletRequest,
                                         @RequestParam("zip") MultipartFile zipFile,
                                         @RequestParam("resultFile") MultipartFile resultFile) throws IOException {
        logger.info("Received file " + zipFile.getOriginalFilename());
        String filePath = servletRequest.getServletContext().getRealPath("/");

        zipFile.transferTo(new File(filePath));
        ProjectResult projectResult = codeParserService.checkCodeResult(filePath, resultFile);
        String fileName = zipFile.getOriginalFilename();
        projectResult.setStudentName(fileName.substring(0, fileName.length()-4));
        return projectResult;
    }

    @PostMapping("/main/zip")
    public ProjectResult checkCodeResult(HttpServletRequest servletRequest,
                                         @RequestParam("mainJavaFile") MultipartFile mainJavaFile,
                                         @RequestParam("zip") MultipartFile zipFile,
                                         @RequestParam("resultFile") MultipartFile resultFile) throws IOException {
        logger.info("Received file " + zipFile.getOriginalFilename());
        codeParserService.putToSource(mainJavaFile);
        String filePath = servletRequest.getServletContext().getRealPath("/");
        zipFile.transferTo(new File(filePath));
        ProjectResult projectResult = codeParserService.checkCodeResult(filePath, resultFile);
        String fileName = zipFile.getOriginalFilename();
        projectResult.setStudentName(fileName.substring(0, fileName.length()-4));
        return projectResult;
    }
}
