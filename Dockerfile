## Build
FROM node:8 as react-build
WORKDIR /app
COPY . ./
RUN npm install \
&& cp -r /app/public/assets/cytoscape /app/public/assets/cytoscape-panzoom /app/public/assets/react-cytoscapejs /app/public/assets/react-scripts node_modules/
RUN npm run build

# Copy build to production
FROM nginx:1.15.9-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=react-build /app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
