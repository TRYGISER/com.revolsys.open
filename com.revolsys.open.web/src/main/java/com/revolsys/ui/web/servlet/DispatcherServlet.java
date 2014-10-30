package com.revolsys.ui.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

import com.revolsys.ui.web.utils.HttpServletUtils;

public class DispatcherServlet extends
org.springframework.web.servlet.DispatcherServlet {
  private static final Logger LOG = LoggerFactory.getLogger(DispatcherServlet.class);

  @Override
  public void destroy() {
    super.destroy();
    final WebApplicationContext webApplicationContext = getWebApplicationContext();
    if (webApplicationContext instanceof AbstractApplicationContext) {
      final AbstractApplicationContext cwac = (AbstractApplicationContext)webApplicationContext;
      cwac.getApplicationListeners().clear();
      final ApplicationEventMulticaster eventMultiCaster = cwac.getBean(
        AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
        ApplicationEventMulticaster.class);
      eventMultiCaster.removeAllListeners();
    }
  }

  @Override
  protected void doService(final HttpServletRequest request,
    final HttpServletResponse response) throws Exception {
    final HttpServletRequest savedRequest = HttpServletUtils.getRequest();
    final HttpServletResponse savedResponse = HttpServletUtils.getResponse();
    try {
      HttpServletUtils.setRequestAndResponse(request, response);
      super.doService(request, response);
      request.removeAttribute(WebUtils.INCLUDE_CONTEXT_PATH_ATTRIBUTE);
      request.removeAttribute(WebUtils.INCLUDE_PATH_INFO_ATTRIBUTE);
      request.removeAttribute(WebUtils.INCLUDE_QUERY_STRING_ATTRIBUTE);
      request.removeAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
      request.removeAttribute(WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE);
    } catch (final NestedServletException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof AccessDeniedException) {
        throw (AccessDeniedException)cause;
      } else {
        LOG.error(e.getMessage(), e);
      }
      throw e;
    } catch (final AccessDeniedException e) {
      throw e;
    } catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      throw e;
    } finally {
      if (savedRequest == null) {
        HttpServletUtils.clearRequestAndResponse();
      } else {
        HttpServletUtils.setRequestAndResponse(savedRequest, savedResponse);
      }
    }
  }
}
