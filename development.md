## Development Guide

The project is based on Java 8 using Jersey as a JAX-RS implementation to provide a REST interface over the list of possible operations described in the REST API section. This operations are implemented in com.atos.indigo.reposync.RepositoryServiceProviderService class. All of them, save the list functionality, will use a Java binding for docker to get the needed images or image info and then execute operations in the backend to create or update said images. 
To simplify the access to the different backend implementations, every one of them has to implement com.atos.indigo.reposync.providers.RepositoryServiceProvider interface with the following operations:

- List<ImageInfoBean> images(String parameters): List images in the platform, both Docker and Virtual Machines with optional filtering as a parameter. This parameter may accept wildcards.

- ActionResponseBean delete(String imageId): Delete an image in the platform given its id. It should return a boolean with the success status of the operation and an optinal message in case of failure.

- ImageInfoBean imageUpdated(String imageName, String tag,
                             InspectImageResponse img, DockerClient client): This operation will signal an update in one of the images in DockerHub. The backend should check if the image already exist and then create or update it with the information provided. 

New backends can be developed as long as they comply with the given interface

## REST API

The following operations are implemented in the service:

```java
/**
   * Get a list of images present in the IaaS platform filtering by name.
   * @param filter Optional filter by name. It should be a regular expression.
   * @return The list of images found.
   */
  @GET
  @Path("images")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Authorized
  public List<ImageInfoBean> images(@QueryParam("filter") String filter);
  
/**
   * Force the download and register of a docker image.
   * @param imageName Image name in Docker Hub.
   * @param tag Desired tag. This parameter is optiona. If not present, latest will be used.
   * @return Return asynchronously the information of the new image.
   */
  @PUT
  @Path("images")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorized
  public ChunkedOutput<ImageInfoBean> pull(
          @QueryParam("imageName") final String imageName,
          @QueryParam("tag") final String tag);

/**
   * Delete an image provided its id.
   * @param imageId Image Id to delete.
   * @return Success status.
   */
  @DELETE
  @Path("images/{imageId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorized
  public ActionResponseBean delete(@PathParam("imageId") String imageId);

/**
   * Webhook that will be called when a new image is pushed or updated to the Indigo repository.
   * @param token The secret token needed to verify the origin of the call.
   * @param payload The webhook payload sent by DockerHub.
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("notify")
  public ChunkedOutput<ImageInfoBean> notify(@QueryParam("token") String token,
                                             ObjectNode payload);
  
/**
   * Force the synchronization of the configured repositories.
   * It will pull the images and then execute an update on each of them
   * @return Asynchronously returns each updated image.
   */
  @PUT
  @Path("sync")
  @Produces(MediaType.APPLICATION_JSON)
  @Authorized
  public ChunkedOutput<String> sync();
  
```

Further operations can be added as needed.
