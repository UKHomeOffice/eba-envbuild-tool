package com.ipt.ebsa.environment.build.plugin;

import hudson.model.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.security.LastGrantedAuthoritiesProperty;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.acegisecurity.GrantedAuthority;

/**
 * Considers all the user's roles and then filters out any environments that don't match them. The algorithm
 * is roughly:
 * 1. Identify roles which have the format env_XXX_crud (discount any others)
 * 2. take the XXX part 
 * 3. Filter out any environments that don't contain "_XXX_" (including the underscores). This is so we don't 
 * 	  get false positives if, for example, the XXX is PR and we have an environment of the form HO_IPT_NP_PRP1
 *
 * @author David Manning
 */
public class EnvironmentFilterer {

	public static final Logger LOG = Logger.getLogger(EnvironmentFilterer.class.getName());

	private static final Pattern ENV_EXTRACTOR = Pattern.compile("^env_(.*)_crud$");
	
	public void filterEnvironments(JSONObject obj, User user) {
		Collection<String> authorisedTo = getRolesForCurrentUser(user);
		Collection<String> filteredRoles = new ArrayList<String>();
		JSONArray envs = obj.getJSONArray("envs");
		
		for (String rawRole : authorisedTo) {
			Matcher matcher = ENV_EXTRACTOR.matcher(rawRole);
			if (matcher.matches()) {
				filteredRoles.add(matcher.group(1));
			}
		}
		List<JSONObject> toRemove = new ArrayList<JSONObject>();
		TOP: for (int i = 0; i < envs.size(); i++) {
			JSONObject object = (JSONObject) envs.get(i);
			
			for (String role : filteredRoles) {
				if (object.containsKey("environment") && (object.getString("environment").toLowerCase().contains("_" + role + "_") ||
														  object.getString("environment").toLowerCase().startsWith(role + "_"))) {
					LOG.log(Level.FINEST, "Keeping environment [" + object.getString("environment") + "]" );
					continue TOP;
				}
				if (object.containsKey("environment")) {
					LOG.log(Level.FINEST, "Removing environment [" + object.getString("environment") + "]" );
				}
			}
			toRemove.add(object);
		}
		envs.removeAll(toRemove);
	}
	
	
	private Collection<String> getRolesForCurrentUser(User user) {
		List<String> roles = new ArrayList<String>();
		
		LastGrantedAuthoritiesProperty property = user.getProperty(LastGrantedAuthoritiesProperty.class);
		
		for (GrantedAuthority grantedAuthority : property.getAuthorities()) {
			roles.add(grantedAuthority.getAuthority());
		}
		
		return roles;
	}
}
