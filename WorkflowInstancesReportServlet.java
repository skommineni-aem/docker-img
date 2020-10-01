package com.cnb.components.core.servlets;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.google.gson.Gson;

@Component(service = Servlet.class, property = { Constants.SERVICE_DESCRIPTION + "= Dynamic Drop Down to overide ",
		"sling.servlet.paths=" + "/bin/getWFData", Constants.SERVICE_RANKING + "=800" })
public class WorkflowInstancesReportServlet extends SlingSafeMethodsServlet {

	private static Logger LOGGER = LoggerFactory.getLogger(WorkflowInstancesReportServlet.class);

	ResourceResolver resolver = null;

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
		try {
			String path = request.getParameter("path");
			resolver = request.getResourceResolver();
			QueryBuilder builder = resolver.adaptTo(QueryBuilder.class);
			Session session = resolver.adaptTo(Session.class);

			Map<String, String> map = new HashMap<>();
			map.put("path", "/var/workflow");
			map.put("type", "cq:Payload");
			map.put("property", "path");
			map.put("property.value", path + "%");
			map.put("property.operation", "like");
			map.put("p.limit", "-1");
			Query query = builder.createQuery(PredicateGroup.create(map), session);
			SearchResult result = query.getResult();
			Iterator<Resource> itr = result.getResources();
			List<WorkflowReportModel> list = new ArrayList<>();

			DateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
			while (itr.hasNext()) {
				Resource payloadRes = itr.next();
				WorkflowReportModel model = new WorkflowReportModel();
				model.setPath(payloadRes.getValueMap().get("path", path));
				Resource wfModelRes = payloadRes.getParent().getParent();
				model.setInitiator(wfModelRes.getValueMap().get("initiator", ""));
				model.setStatus(wfModelRes.getValueMap().get("status", ""));
				model.setModelName(wfModelRes.getName());
				Calendar startTimeCalendar = wfModelRes.getValueMap().get("startTime", Calendar.class);
				Calendar endTimeCalendar = wfModelRes.getValueMap().get("endTime", Calendar.class);
				if (startTimeCalendar != null)
					model.setStartTime(outputFormat.format(startTimeCalendar.getTime()));
				if (endTimeCalendar != null)
					model.setEndTime(outputFormat.format(endTimeCalendar.getTime()));
				list.add(model);
			}

			Gson gson = new Gson();
			String jsonString = gson.toJson(list);
			response.getOutputStream().println(jsonString);
		} catch (Exception e) {
			LOGGER.error("Error in Get Drop Down Values", e);
		}
	}

}
