package com.big.core;

import io.minio.*;
import io.minio.errors.InvalidExpiresRangeException;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动创建对象
 *
 * @author Yin
 * @date 2024-09-25 9:06
 */

public class MinioClientUtil {

    private MinioClient minioClient;

    public MinioClientUtil() {
    }

    public MinioClientUtil(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    private static final int DEFAULT_EXPIRY_TIME = 7 * 24 * 3600;

    public boolean bucketExists(String bucketName) throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName)
                .build());
    }

    public void makeBucket(String bucketName) throws Exception {
        if (!bucketExists(bucketName)) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build());
        }
    }

    public List<String> listBucketNames() throws Exception {
        List<Bucket> bucketList = listBuckets();
        List<String> bucketListName = new ArrayList<>();
        for (Bucket bucket : bucketList) {
            bucketListName.add(bucket.name());
        }
        return bucketListName;
    }

    public List<Bucket> listBuckets() throws Exception {
        return minioClient.listBuckets();
    }

    public boolean removeBucket(String bucketName) throws Exception {
        boolean flag = bucketExists(bucketName);
        if (flag) {
            Iterable<Result<Item>> myObjects = listObjects(bucketName);
            for (Result<Item> result : myObjects) {
                Item item = result.get();
                // 有对象文件，则删除失败
                if (item.size() > 0) {
                    return false;
                }
            }
            // 删除存储桶，注意，只有存储桶为空时才能删除成功。
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
            flag = bucketExists(bucketName);
            if (!flag) {
                return true;
            }

        }
        return false;
    }

    public List<String> listObjectNames(String bucketName) throws Exception {
        List<String> listObjectNames = new ArrayList<>();
        boolean flag = bucketExists(bucketName);
        if (flag) {
            Iterable<Result<Item>> myObjects = listObjects(bucketName);
            for (Result<Item> result : myObjects) {
                Item item = result.get();
                listObjectNames.add(item.objectName());
            }
        }
        return listObjectNames;
    }

    public Iterable<Result<Item>> listObjects(String bucketName) throws Exception {
        boolean flag = bucketExists(bucketName);
        if (flag) {
            return minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build());
        }
        return null;
    }

    public boolean putObject(String bucketName, String objectName, InputStream stream, String contentType) throws Exception {
        if (!bucketExists(bucketName)) {
            makeBucket(bucketName);
        }
        minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                stream, stream.available(), -1)
                .contentType(contentType)
                .build());
        ObjectStat statObject = statObject(bucketName, objectName);
        if (statObject != null && statObject.length() > 0) {
            return true;
        }
        return false;
    }

    public InputStream getObject(String bucketName, String objectName) throws Exception {

        boolean flag = bucketExists(bucketName);

        if (flag) {
            ObjectStat statObject = statObject(bucketName, objectName);
            if (statObject != null && statObject.length() > 0) {
                InputStream stream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
                return stream;
            }
        }
        return null;
    }

    public boolean removeObject(String bucketName, String objectName) throws Exception {
        boolean flag = bucketExists(bucketName);
        if (flag) {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        }
        return false;

    }

    public List<String> removeObject(String bucketName, List<DeleteObject> objects) throws Exception {
        List<String> deleteErrorNames = new ArrayList<>();
        boolean flag = bucketExists(bucketName);
        if (flag) {
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(bucketName)
                    .objects(objects)
                    .build());
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                deleteErrorNames.add(error.objectName());
            }
        }
        return deleteErrorNames;
    }

    public String getPreSignedObjectUrl(String bucketName, String objectName, Integer expires) throws Exception {
        boolean flag = bucketExists(bucketName);
        String url = "";
        if (flag) {
            if (expires < 1 || expires > DEFAULT_EXPIRY_TIME) {
                throw new InvalidExpiresRangeException(expires,
                        "expires must be in range of 1 to " + DEFAULT_EXPIRY_TIME);
            }
            url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(expires)
                    .build());
        }
        return url;
    }

    public String getPreSignedObjectGetPutUrl(String bucketName, String objectName, Integer expires) throws Exception {
        boolean flag = bucketExists(bucketName);
        String url = "";
        if (flag) {
            if (expires < 1 || expires > DEFAULT_EXPIRY_TIME) {
                throw new InvalidExpiresRangeException(expires,
                        "expires must be in range of 1 to " + DEFAULT_EXPIRY_TIME);
            }
            Map<String, String> reqParams = new HashMap<String, String>();
            reqParams.put("response-content-type", "application/json");
            url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expires)
                            .extraQueryParams(reqParams)
                            .build());
        }
        return url;
    }

    public ObjectStat statObject(String bucketName, String objectName) throws Exception {
        boolean flag = bucketExists(bucketName);
        if (flag) {
            ObjectStat statObject = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName).build());
            return statObject;
        }
        return null;
    }

    public String getObjectUrl(String bucketName, String objectName) throws Exception {
        boolean flag = bucketExists(bucketName);
        String url = "";
        if (flag) {
            url = minioClient.getObjectUrl(bucketName, objectName);
        }
        return url;
    }

    public void setBucketPolicy(String bucketName, String policyJson) throws Exception {
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policyJson)
                        .build());
    }

}
