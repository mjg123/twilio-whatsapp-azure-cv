import static spark.Spark.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.net.URI;

public class App {

    private static final String NO_IMAGE_MESSAGE =
            "<Response><Message>I can't help if you don't send an image \uD83D\uDE09</Message></Response>";
    private static final String NO_DESCRIPTION_MESSAGE =
            "<Response><Message>Sorry, I couldn't describe that \uD83D\uDE23</Message></Response>";

    public static void main(String[] args) {
        get("/", (req, res) -> "Hello \uD83D\uDC4B");

        post("/msg", (req, res) -> {

            String mediaUrl = req.queryParams("MediaUrl0");
            if (mediaUrl == null) return NO_IMAGE_MESSAGE;

            String description = getAzureCVDescription(mediaUrl);
            if (description == null) return NO_DESCRIPTION_MESSAGE;

            // Return TwiML to send the description back to WhatsApp
            return "<Response><Message>It’s " + description + "</Message></Response>";
        });
    }

    private static String getAzureCVDescription(String mediaUrl) {

        // Replace <Subscription Key> with your valid subscription key.
        // SECURITY WARNING: Do NOT commit this to GitHub or put it anywhere public
        String subscriptionKey = "<Subscription Key>";

        String uriBase = "https://westcentralus.api.cognitive.microsoft.com/vision/v2.0/analyze";

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        try {
            URIBuilder builder = new URIBuilder(uriBase);

            // Request parameters. All of them are optional.
            builder.setParameter("visualFeatures", "Description");
            builder.setParameter("language", "en");

            // Prepare the URI for the REST API method.
            URI uri = builder.build();
            HttpPost request = new HttpPost(uri);

            // Request headers.
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);

            // Request body.
            StringEntity requestEntity =
                    new StringEntity("{\"url\":\"" + mediaUrl + "\"}");
            request.setEntity(requestEntity);

            // Call the REST API method and get the response entity.
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                // Format and display the JSON response.
                String jsonString = EntityUtils.toString(entity);
                JSONObject json = new JSONObject(jsonString);

                // This extracts the caption from the JSON returned by Azure CV
                return json
                        .getJSONObject("description")
                        .getJSONArray("captions")
                        .getJSONObject(0)
                        .getString("text");

            }
        } catch (Exception e) {
            // Display error message.
            System.out.println(e.getMessage());
        }
        return null;
    }

}
