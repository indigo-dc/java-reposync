package com.atos.indigo.reposync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.image.ImageService;
import org.openstack4j.core.transport.Config;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.image.ContainerFormat;
import org.openstack4j.model.image.DiskFormat;
import org.openstack4j.model.image.Image;
import org.openstack4j.openstack.OSFactory;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    public static final String DOCKER_ID = "dockerId";

    private JsonBuilderFactory factory = Json.createBuilderFactory(null);
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private Map<String, ImageService> clients = new HashMap<>();

    private ImageService getClient(String username, String password) {

        OSClient client = OSFactory.builderV3().
                endpoint(ENDPOINT).
                credentials(username,password, Identifier.byName(DOMAIN)).
                withConfig(Config.DEFAULT.withSSLVerificationDisabled()).
                scopeToProject(Identifier.byName(PROJECT),Identifier.byName(PROJECT_DOMAIN)).authenticate();
        return client.images();
    }

    @Override
    public String login(String username, String password) {

        String token = AuthorizationManager.generateToken(username,password);

        clients.put(token,getClient(username, password));

        return token;
    }

    @Override
    public String logout(String token) {
        if (clients.get(token) != null) {
            clients.remove(token);
        }

        return token;
    }

    @Override
    public String images(String token, String filter) {
        ImageService api = clients.get(token);
        if (api != null) {
            List<? extends Image> images = api.listAll();
            if (filter != null) {
                Pattern pattern = Pattern.compile(filter);
                if (pattern != null) {
                    images.removeIf(image -> !pattern.matcher(image.getName()).matches());
                }
            }
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(images);
            } catch (JsonProcessingException e) {
                throw new InternalServerErrorException("Can't serialize Openstack response");
            }
        }
        return null;
    }

    @Override
    public String pull(String imageName) {
        return null;
    }

    @Override
    public String push(String imageData) {
        return null;
    }

    @Override
    public String delete(String imageId) {
        return null;
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
            ImageService api = getClient(adminUser, adminPass);
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

    private void addImagge(String image, DockerClient restClient) {

    }

    private void addImage(com.github.dockerjava.api.model.Image img, ImageService api, DockerClient restClient) throws IOException {

        System.out.println("Adding image "+img.getId());

        InspectImageResponse details = restClient.inspectImageCmd(img.getId()).exec();

        InputStream responseStream = restClient.saveImageCmd(img.getId()).exec();

        System.out.println("Image read "+responseStream.available()+" bytes");

        org.openstack4j.model.common.Payload payload = Payloads.create(responseStream);
        Image result = api.create(Builders.image().name(details.getRepoTags().get(0).split(":")[0])
                .containerFormat(ContainerFormat.DOCKER)
                .diskFormat(DiskFormat.RAW)
                .property(DOCKER_ID,img.getId())
                .property("hypervisor_type","docker").build(),payload);
        System.out.println("Created image "+result.getId());

    }
}
