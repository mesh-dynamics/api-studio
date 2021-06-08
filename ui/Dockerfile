# Build
FROM node:12 as react-build
WORKDIR /app
COPY . ./
RUN npm install
RUN npm run build

# Copy build to production
FROM nginx:1.15.9-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=react-build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
