package com.atos.indigo.reposync.providers;

import com.atos.indigo.reposync.beans.ActionResponseBean;
import com.atos.indigo.reposync.beans.ImageInfoBean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.image.ImageService;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.compute.ActionResponse;
import org.openstack4j.model.image.ContainerFormat;
import org.openstack4j.model.image.DiskFormat;
import org.openstack4j.model.image.Image;
import org.openstack4j.openstack.OSFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by jose on 25/04/16.
 */
public class OpenStackRepositoryServiceProvider implements RepositoryServiceProvider {

    private static final String ENDPOINT = System.getenv("OS_AUTH_URL");
    private static final String PROJECT_DOMAIN = System.getenv("OS_PROJECT_DOMAIN_NAME");
    private static final String PROJECT = System.getenv("OS_PROJECT_NAME");
    private static final String DOMAIN = System.getenv("OS_USER_DOMAIN_NAME");

    private static final String ADMIN_USER_VAR = "OS_USERNAME";
    private static final String ADMIN_PASS_VAR = "OS_PASSWORD";
    public static final String DOCKER_ID = "dockerid";
    public static final String DOCKER_TAGS = "dockertags";


    private ObjectMapper mapper = new ObjectMapper();

    private OSClient getClient(String username, String password) {

        OSClient client = OSFactory.builderV3().
                endpoint(ENDPOINT).
                credentials(username,password, Identifier.byName(DOMAIN)).
                withConfig(Config.DEFAULT.withSSLVerificationDisabled()).
                scopeToProject(Identifier.byName(PROJECT),Identifier.byName(PROJECT_DOMAIN)).authenticate();
        return client;
    }

    private ImageService getAdminClient(){
        String adminUser = System.getenv(ADMIN_USER_VAR);
        String adminPass = System.getenv(ADMIN_PASS_VAR);

        if (adminUser != null && adminPass != null) {
            return getClient(adminUser, adminPass).images();
        } else {
            return null;
        }
    }


    @Override
    public List<ImageInfoBean> images(String filter) {
        List<ImageInfoBean> result = new ArrayList<>();
        ImageService api = getAdminClient();
        if (api != null) {
            List<? extends Image> images = api.listAll();
            Stream<? extends Image> listStream = images.stream();
            if (filter != null) {
                Pattern pattern = Pattern.compile(filter);
                if (pattern != null) {
                    listStream.
                            filter(image -> pattern.matcher(image.getName()).matches());
                }
            }
            result = listStream.map(image -> getImageInfo(image)).collect(Collectors.toList());
        }
        return result;
    }

    private ImageInfoBean getImageInfo(Image image) {
        ImageInfoBean result = new ImageInfoBean();
        result.setId(image.getId());
        result.setName(image.getName());
        Map<String, String> imgProps = image.getProperties();

        if (imgProps != null && imgProps.get(DOCKER_ID) != null) {
            result.setType(ImageInfoBean.ImageType.DOCKER);
            if (imgProps.get(DOCKER_TAGS) != null) {
                try {
                   result.setTags(mapper.readValue(imgProps.get(DOCKER_TAGS), new TypeReference<List<String>>(){}));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            result.setType(ImageInfoBean.ImageType.VM);
        }
        return result;
    }

    @Override
    public String pull(String imageName) {
        return null;
    }


    @Override
    public ActionResponseBean delete(String imageId) {
        ImageService client = getAdminClient();
        ActionResponse opResult = client.delete(imageId);
        ActionResponseBean result = new ActionResponseBean();
        result.setSuccess(opResult.isSuccess());
        result.setErrorMessage(opResult.getFault());
        return result;
    }

    @Override
    public String log(String parameters) {
        return null;
    }

    @Override
    public String space() {
        return null;
    }

    @Override
    public String external() {
        return null;
    }

    @Override
    public String externalSearch(String repoId) {
        return null;
    }

    @Override
    public String externalPull(String repoId, String imageId) {
        return null;
    }


    @Override
    public String sync(List<com.github.dockerjava.api.model.Image> imageSummaries, DockerClient dockerClient) {
        String adminUser = System.getenv(ADMIN_USER_VAR);
        String adminPass = System.getenv(ADMIN_PASS_VAR);

        if (adminUser != null && adminPass != null) {
            ImageService api = getClient(adminUser, adminPass).images();
            List<? extends Image> imageList = api.listAll();


            List<com.github.dockerjava.api.model.Image> toAdd = imageSummaries.stream().
                    filter(img -> !imageList.stream().
                            anyMatch(osImage -> img.getId().equals(osImage.getProperties().get(DOCKER_ID)))).
                    collect(Collectors.toList());

            for (com.github.dockerjava.api.model.Image img : toAdd) {
                try {
                    addImage(img, api, dockerClient);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }



        return null;
    }

    @Override
    public ImageInfoBean imageUpdated(InspectImageResponse img, DockerClient restClient) {
        ImageService client = getAdminClient();
        boolean create = true;
        if (client != null) {
            Optional<? extends Image> foundImg = client.listAll().stream().filter(image -> image.getProperties() != null && img.getId().equals(image.getProperties().get(DOCKER_ID))).findFirst();
            if (foundImg.isPresent()) {
                ActionResponse response = client.delete(foundImg.get().getId());
                if (!response.isSuccess()) {
                    System.out.println("Error deleting updated image: "+response.getFault());
                    create = false;
                }
            }

            if (create) {
                try {
                    Image added = addImage(img, client, restClient);
                    if (added != null) {
                        return getImageInfo(added);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private Image addImage(com.github.dockerjava.api.model.Image img, ImageService api, DockerClient restClient) throws IOException {

        System.out.println("Adding image "+img.getId());

        InspectImageResponse details = restClient.inspectImageCmd(img.getId()).exec();

        return addImage(details,api,restClient);

    }

    private Image addImage(InspectImageResponse details, ImageService api, DockerClient restClient) throws IOException {
        InputStream responseStream = restClient.saveImageCmd(details.getId()).exec();

        System.out.println("Image read "+responseStream.available()+" bytes");

        org.openstack4j.model.common.Payload payload = Payloads.create(responseStream);
        Image result = api.create(Builders.image().name(details.getRepoTags().get(0).split(":")[0])
                .containerFormat(ContainerFormat.DOCKER)
                .diskFormat(DiskFormat.RAW)
                .property(DOCKER_ID,details.getId())
                .property(DOCKER_TAGS,serializeNames(details.getRepoTags()))
                .property("hypervisor_type","docker").build(),payload);
        System.out.println("Created image "+result.getId());
        return result;
    }

    private String serializeNames(List<String> repoTags) {
        try {
            return mapper.writeValueAsString(repoTags);
        } catch (JsonProcessingException e) {
            System.out.println("Error serializing repository tags");
            e.printStackTrace();
        }
        return null;
    }
}
