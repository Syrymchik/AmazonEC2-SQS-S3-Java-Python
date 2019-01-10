package aws.cloud.sqs.Controller;

import aws.cloud.sqs.Entity.Result;
import aws.cloud.sqs.Entity.User;
import aws.cloud.sqs.Service.S3Services;
import aws.cloud.sqs.Service.ServiceSQS;
import aws.cloud.sqs.Validator.Validator1;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class MainController {

    @Autowired
    private ServiceSQS serviceSQS;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${bucket.name}")
    private String bucketName;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private S3Services s3Services;

    public static final String topic = "requestQueue";

    String url = "aws:s3:::telecomecloudcomputing";

    @GetMapping
    public String GetHome(Model model) throws IOException {
        model.addAttribute(new User());
        if (ServiceSQS.results.size() != 0) {
            List<Result> results = new ArrayList<Result>();
            results.addAll(ServiceSQS.results);
            model.addAttribute("results", results);
        }
        return "home";
    }

    @PostMapping
    public String PostHome(@ModelAttribute @Valid User user, BindingResult bindingResult, Model model) throws IOException {

        if (bindingResult.hasErrors()) {
            Map<String, String> errorsMap = Validator1.getErrors(bindingResult);
            model.mergeAttributes(errorsMap);
        } else {
            serviceSQS.send(topic, objectMapper.writeValueAsString(user));
        }
//        if (!StringUtils.isEmpty(ServiceSQS.results)) {
        if (ServiceSQS.results.size() != 0) {
            List<Result> results = new ArrayList<Result>();
            results.addAll(ServiceSQS.results);
            model.addAttribute("results", results);
        }
        return "home";
    }

    @ResponseBody
    @GetMapping("results")
    public List<Result> getResults(){
        if (ServiceSQS.results.size() != 0)
            return ServiceSQS.results;
        return new ArrayList<Result>();
    }


    @GetMapping("files")
    public String getFiles(Model model) {
        List<String> keys = getStrings();
        List<String> keysNewFiles = ServiceSQS.keysNewFiles;
        model.addAttribute("keys", keys);
        model.addAttribute("keysNewFiles", keysNewFiles);
        return "files";
    }

    @PostMapping("files")
    public String postFiles(@RequestParam("name") String name, @RequestParam("file") MultipartFile file, Model model) {

        if (!StringUtils.isEmpty(name) && !StringUtils.isEmpty(file)) {
            s3Services.uploadFile(name, file);
            model.addAttribute("mess", String.format("File with name %s successfuly uploaded", file.getOriginalFilename()));
        } else {
            model.addAttribute("mess", "File or name of key is empty");
        }

        List<String> keys = getStrings();
        List<String> keysNewFiles = ServiceSQS.keysNewFiles;
        model.addAttribute("keys", keys);
        model.addAttribute("keysNewFiles", keysNewFiles);
        return "files";
    }

    @GetMapping("getfile/{key}")
    public ResponseEntity<byte[]> getFileByKey(@PathVariable("key") String key){
        ByteArrayOutputStream downloadInputStream = s3Services.downloadFile(key);

        return ResponseEntity.ok()
                .contentType(contentType(key))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"" + key + "\"")
                .body(downloadInputStream.toByteArray());
    }

    private MediaType contentType(String keyname) {
        String[] arr = keyname.split("\\.");
        String type = arr[arr.length-1];
        switch(type) {
            case "txt": return MediaType.TEXT_PLAIN;
            case "png": return MediaType.IMAGE_PNG;
            case "jpg": return MediaType.IMAGE_JPEG;
            default: return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private List<String> getStrings() {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName);
        List<String> keys = new ArrayList<>();
        ObjectListing objectListing = amazonS3.listObjects(listObjectsRequest);

        while (true) {
            List<S3ObjectSummary> summaries = objectListing.getObjectSummaries();
            if (summaries.size() < 1) {
                break;
            }

            for (S3ObjectSummary item : summaries) {
                if (!item.getKey().endsWith("/"))
                    keys.add(item.getKey());
            }

            objectListing = amazonS3.listNextBatchOfObjects(objectListing);
        }
        return keys;
    }


}
