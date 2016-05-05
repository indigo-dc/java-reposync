package com.atos.indigo.reposync;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import org.opennebula.client.OneResponse;
import org.opennebula.client.datastore.Datastore;
import org.opennebula.client.datastore.DatastorePool;
import org.opennebula.client.image.ImagePool;

import javax.ws.rs.PathParam;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jose on 20/04/16.
 */
public class OpenNebulaRepositoryServiceProvider implements RepositoryServiceProvider {

    public static final String ENDPOINT = System.getenv("ONE_ENDPOINT");
    public static final String ONEDOCK_DATASTORE_NAME = "onedock";
    public static final String DOCKER_PREFIX = "docker://";

    Map<String, Client> providers = new HashMap<String,Client>();

    private Client createClient(String username, String password) throws ClientConfigurationException {
        String authToken = username + ":" + password;
        Client provider = new Client(authToken, ENDPOINT);
        return provider;
    }

    @Override
    public String login(String username, String password) {
        String authToken = username + ":" + password;
        try {
            Client imgPool = createClient(username, password);
            if (imgPool != null) {
                String token = AuthorizationManager.generateToken(username, password);
                providers.put(token,imgPool);
                return token;
            }

        } catch (ClientConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String logout(String token) {
        return null;
    }

    @Override
    public String images(String token, String parameters) {
        return null;
    }

    @Override
    public String pull(@PathParam("imageName") String imageName) {
        return null;
    }

    @Override
    public String push(String imageData) {
        return null;
    }

    @Override
    public String delete(@PathParam("imageId") String imageId) {
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
    public String externalSearch(@PathParam("repoId") String repoId) {
        return null;
    }

    @Override
    public String externalPull(@PathParam("repoId") String repoId, @PathParam("imageId") String imageId) {
        return null;
    }

    private String getImageName(Image image) {
        if (image.getRepoTags() != null && image.getRepoTags().length > 0) {
            return image.getRepoTags()[0].split(":")[0];
        } else {
            return "";
        }
    }


    @Override
    public String sync(List<Image> imageSummaries, DockerClient client) {

        try {
            Client oneClient = createClient(System.getenv("ONE_USER"), System.getenv("ONE_PASS"));

            DatastorePool dataPool = new DatastorePool(oneClient);
            OneResponse poolInfo = dataPool.info();
            Iterator<Datastore> poolResponse = dataPool.iterator();
            Integer dockerStoreId = null;
            while(poolResponse != null && poolResponse.hasNext() && dockerStoreId == null) {
                Datastore store = poolResponse.next();
                if (ONEDOCK_DATASTORE_NAME.equals(store.getName())) {
                    dockerStoreId = store.id();
                }
            }

            List<String> existing = new ArrayList<>();
            if (dockerStoreId != null) {

                ImagePool imgPool = new ImagePool(oneClient);
                OneResponse info = imgPool.info();
                System.out.println(info.getMessage());
                Iterator<org.opennebula.client.image.Image> imgResponse = imgPool.iterator();
                while (imgResponse != null && imgResponse.hasNext()) {
                    org.opennebula.client.image.Image img = imgResponse.next();
                    String datastore = img.xpath("DATASTORE_ID");
                    if (datastore != null) {
                        if (dockerStoreId.equals(Integer.parseInt(datastore))) {
                            String imgName = img.xpath("PATH");
                            if (imgName != null && imgName.startsWith(DOCKER_PREFIX)) {
                                existing.add(imgName.substring(DOCKER_PREFIX.length()));
                            }
                        }
                    }
                }


                List<String> toAdd = imageSummaries.stream().
                        map(image -> getImageName(image))
                        .filter(
                            imageName ->
                                !existing.contains(imageName)).collect(Collectors.toList());

                final Integer storeId = dockerStoreId;
                toAdd.forEach(imageName -> addImage(imageName, oneClient, storeId));

            }
        } catch (ClientConfigurationException e) {
            e.printStackTrace();
        }


        return null;

    }

    private void addImage(String imageName, Client oneClient, Integer dockerStoreId) {

        System.out.println("Creating image "+imageName);
        String name = imageName.replace("/", "_");
        String template = new TemplateGenerator()
                .addProperty("NAME","\""+name+"\"")
                .addProperty("PATH","docker://"+imageName)
                .addProperty("DESCRIPTION","\"Docker image\"").generate();

        OneResponse result = org.opennebula.client.image.Image.allocate(oneClient, template, dockerStoreId);
        if (result.isError()) {
            System.out.println(result.getErrorMessage());
        } else {
            System.out.println(result.getMessage());
        }


    }

    private class TemplateGenerator {
        StringBuilder builder = new StringBuilder();

        public TemplateGenerator addProperty(String name, String value) {
            builder.append(name);
            builder.append("=");
            builder.append(value);
            builder.append("\n");
            return this;
        }

        public String generate() {
            return builder.toString();
        }
    }


}
