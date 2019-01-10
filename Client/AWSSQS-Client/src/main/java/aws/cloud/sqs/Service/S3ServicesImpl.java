package aws.cloud.sqs.Service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class S3ServicesImpl implements S3Services  {

    @Autowired
    private AmazonS3 s3client;

    @Autowired
    private ServiceSQS serviceSQS;

    @Value("${bucket.name}")
    private String bucketName;

    @Override
    public ByteArrayOutputStream downloadFile(String keyName) {
        try {
            S3Object s3object = s3client.getObject(new GetObjectRequest(bucketName, keyName));

            InputStream is = s3object.getObjectContent();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len;
            byte[] buffer = new byte[4096];
            while ((len = is.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, len);
            }

            return baos;
        } catch (IOException ioe) {
            System.out.println("IOException: " + ioe.getMessage());
        } catch (AmazonServiceException ase) {
            System.out.println("sCaught an AmazonServiceException from GET requests, rejected reasons:");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
            throw ase;
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException: ");
            System.out.println("Error Message: " + ace.getMessage());
            throw ace;
        }

        return null;
    }

    @Override
    public void uploadFile(String keyName, MultipartFile file) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            s3client.putObject(bucketName, keyName, file.getInputStream(), metadata);
            serviceSQS.send("inbox", keyName);
        } catch(IOException ioe) {
            System.out.println("IOException: " + ioe.getMessage());
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException from PUT requests, rejected reasons:");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
            throw ase;
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException: ");
            System.out.println("Error Message: " + ace.getMessage());
            throw ace;
        }
    }
}
