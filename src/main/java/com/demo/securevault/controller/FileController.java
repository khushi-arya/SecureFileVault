package com.demo.securevault.controller;

import com.demo.securevault.exception.FileNotFoundException;
import com.demo.securevault.model.FileMetadata;
import com.demo.securevault.service.EmailService;
import com.demo.securevault.service.FileService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;


@CrossOrigin(
        origins = "*",
        exposedHeaders = "Content-Disposition"
)
@RestController
public class FileController {


    private final FileService fileService;
    private final EmailService emailService;


    public FileController(FileService fileService,
                          EmailService emailService) {

        this.fileService = fileService;
        this.emailService = emailService;
    }



    // ================= UPLOAD =================

    @PostMapping("/api/files/upload")
    public ResponseEntity<Map<String,Object>> uploadFile(

            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password,
            @RequestParam("userEmail") String userEmail

    ){

        validateUploadRequest(file,password,userEmail);


        String uniqueKey =
                UUID.randomUUID()
                + "_"
                + file.getOriginalFilename();


        fileService.uploadToSupabase(
                file,
                uniqueKey,
                password
        );


        FileMetadata savedMetadata =
                fileService.saveMetadata(
                        file.getOriginalFilename(),
                        uniqueKey,
                        userEmail,
                        file.getContentType()
                );



        try {

            emailService.sendFileLink(
                    userEmail,
                    savedMetadata.getId().toString(),
                    file.getOriginalFilename()
            );

        }catch(Exception e){

            System.out.println(
                    "Email failed : "
                    + e.getMessage()
            );
        }



        return ResponseEntity.ok(
                Map.of(
                        "fileId",
                        savedMetadata.getId().toString(),

                        "message",
                        "File uploaded successfully"
                )
        );

    }





    // ================= DOWNLOAD =================


    @GetMapping("/api/files/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(

            @PathVariable UUID fileId,

            @RequestParam("password") String password

    ){


        FileMetadata metadata =
                fileService.findById(fileId)

                .orElseThrow(
                        () ->
                        new FileNotFoundException(
                                "File metadata not found"
                        )
                );



        byte[] fileBytes =
                fileService.downloadFromSupabase(
                        metadata.getSupabaseKey(),
                        password
                );



        HttpHeaders headers =
                new HttpHeaders();



        // Original uploaded filename

        String fileName =
                metadata.getOriginalFilename();



        // Safety fallback

        if(fileName == null || fileName.isBlank()){

            fileName = "downloaded_file";

        }



        headers.setContentDisposition(

                ContentDisposition
                        .attachment()
                        .filename(fileName)
                        .build()

        );



        // Use the ORIGINAL content-type saved at upload time.
        // This makes downloads work correctly for ANY file format
        // (images, docs, zips, videos, etc.) — not just PDFs.

        String contentType = metadata.getContentType();

        if (contentType != null && !contentType.isBlank()) {

            try {
                headers.setContentType(MediaType.parseMediaType(contentType));
            } catch (Exception e) {
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            }

        } else {

            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        }



        return ResponseEntity.ok()

                .headers(headers)

                .body(fileBytes);

    }






    // ================= VALIDATION =================


    private void validateUploadRequest(

            MultipartFile file,
            String password,
            String userEmail

    ){


        if(file == null || file.isEmpty()){

            throw new IllegalArgumentException(
                    "File must not be empty"
            );

        }



        long maxSize =
                50 * 1024 * 1024;



        if(file.getSize() > maxSize){

            throw new IllegalArgumentException(
                    "File size exceeds 50MB"
            );

        }



        if(password == null || password.length() < 6){

            throw new IllegalArgumentException(
                    "Password must be at least 6 characters"
            );

        }



        if(!isValidEmail(userEmail)){

            throw new IllegalArgumentException(
                    "Invalid email"
            );

        }


    }




    private boolean isValidEmail(String email){


        if(email == null || email.isEmpty()){

            return false;

        }


        String regex =
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";


        return Pattern
                .compile(regex)
                .matcher(email)
                .matches();

    }




    private String formatFileSize(long bytes){

        if(bytes <=0)
            return "0 B";


        String[] units =
                {"B","KB","MB","GB"};


        int index =
                (int)
                (Math.log10(bytes)
                /
                Math.log10(1024));


        return String.format(
                "%.2f %s",
                bytes / Math.pow(1024,index),
                units[index]
        );

    }

}