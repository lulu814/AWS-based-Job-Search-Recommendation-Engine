package external;

import java.util.List;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;

public class GitHubClient {
	// add constants
	// constants usually use static final (won't change)
	private static final String URL_TEMPLATE = "https://jobs.github.com/positions.json?description=%s&lat=%s&long=%s";
	// 给用户默认的岗位
	private static final String DEFAULT_KEYWORD = "developer";
	
	// add methods search which calls GitHub Job API and return jobs info
	public List<Item> search(double lat, double lon, String keyword) {
		if (keyword == null) {
			keyword = DEFAULT_KEYWORD;
		}
		try {
			// use UTF-8 format to encode (汉字也可以）
			// why encode: e.g. in URL, we replace space with '+' but if we have + in our string in the URL, it will be 
			//replaced with UTF-8 encode
			keyword = URLEncoder.encode(keyword, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String url = String.format(URL_TEMPLATE, keyword, lat, lon);
		// from apache, create a client 
		CloseableHttpClient httpClient = HttpClients.createDefault();
		try {
			// get the response based on the url template after filled in parameters
			CloseableHttpResponse response = httpClient.execute(new HttpGet(url));
			// if not response not valid (status code), return empty
			if (response.getStatusLine().getStatusCode() != 200) {
				return new ArrayList<>();
			}
			// entity might include meta data, we care more about the content
			HttpEntity entity = response.getEntity();
			if (entity == null) {
				return new ArrayList<>();
			}
			// read a stream instead of a big block of data
			// inputstreamreader can only read char by char, so we use bufferedreader which read line by line
			BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
			// after we get the reader, build the response
			StringBuilder responseBody = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				responseBody.append(line);
			}
			reader.close();
			JSONArray array = new JSONArray(responseBody.toString());
			return getItemList(array);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	private List<Item> getItemList(JSONArray array) {
		List<Item> itemList = new ArrayList<>();
		
		//
		List<String> descriptionList = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			// We need to extract keywords from description since GitHub API
			// doesn't return keywords.
			String description = getStringFieldOrEmpty(array.getJSONObject(i), "description");
			// some cases would have empty description but have everything in title
			if (description.equals("") || description.equals("\n")) {
				descriptionList.add(getStringFieldOrEmpty(array.getJSONObject(i), "title"));
			} else {
				descriptionList.add(description);
			}	
		}

		// We need to get keywords from multiple text in one request since
		// MonkeyLearnAPI has limitations on request per minute.
		List<List<String>> keywords = MonkeyLearnClient
				// extractKeywords returns string array, so need to convert it to string list
				.extractKeywords(descriptionList.toArray(new String[descriptionList.size()]));

		for (int i = 0; i < array.length(); ++i) {
			JSONObject object = array.getJSONObject(i);
			ItemBuilder builder = new ItemBuilder();
			
			builder.setItemId(getStringFieldOrEmpty(object, "id"));
			builder.setName(getStringFieldOrEmpty(object, "title"));
			builder.setAddress(getStringFieldOrEmpty(object, "location"));
			builder.setUrl(getStringFieldOrEmpty(object, "url"));
			builder.setImageUrl(getStringFieldOrEmpty(object, "company_logo"));
			// use hashset to get unique elements de-duplicate
			builder.setKeywords(new HashSet<String>(keywords.get(i)));
			
			Item item = builder.build();
			itemList.add(item);
		}
		return itemList;
	}
	
	private String getStringFieldOrEmpty(JSONObject obj, String field) {
		return obj.isNull(field) ? "" : obj.getString(field);
	}


	
}
