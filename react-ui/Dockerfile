# Build
FROM node:8 as react-build
WORKDIR /app
COPY . ./
RUN npm install
RUN npm run build

# Copy build to production
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=react-build /app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
