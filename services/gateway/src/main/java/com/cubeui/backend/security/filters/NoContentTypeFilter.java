package com.cubeui.backend.security.filters;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/*
    Wrapper over HttpServlet class which takes care of removing the content-type = * /*
*/
class RemoveAllMediaTypeReqWrapper extends HttpServletRequestWrapper {
    private boolean noOp;
    private HttpHeaders headers;
    public RemoveAllMediaTypeReqWrapper(HttpServletRequest req){
        super(req);
        String contentType = req.getContentType();
        if(!(contentType!=null && contentType.equals(MediaType.ALL_VALUE))){
            noOp = true;
            return;
        }

        this.headers = new HttpHeaders();
        Enumeration names = _getHttpServletRequest().getHeaderNames();

        String requestEncoding;
        while(names.hasMoreElements()) {
            requestEncoding = (String)names.nextElement();
            if(requestEncoding.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) continue;   //Remove content type

            Enumeration headerValues = _getHttpServletRequest().getHeaders(requestEncoding);

            while(headerValues.hasMoreElements()) {
                String headerValue = (String)headerValues.nextElement();
                this.headers.add(requestEncoding, headerValue);
            }
        }


    }
    private HttpServletRequest _getHttpServletRequest() {
        return (HttpServletRequest)super.getRequest();
    }

    @Override
    public String getContentType(){
        return noOp ? super.getContentType() : null;
    }

    @Override
    public String getHeader(String header){
        if(noOp) return super.getHeader(header);
        return headers.getFirst(header);
    }

    @Override
    public Enumeration<String> getHeaderNames(){
        if(noOp) return super.getHeaderNames();
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String header){
        if(noOp) return super.getHeaders(header);

        List<String> list = headers.get(header);
        return list==null ? Collections.enumeration(Collections.EMPTY_LIST) : Collections.enumeration(list);
    }
}

public class NoContentTypeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) servletRequest;
        filterChain.doFilter(new RemoveAllMediaTypeReqWrapper(req) , servletResponse);
    }
}
