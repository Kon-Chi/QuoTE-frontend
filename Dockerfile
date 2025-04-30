FROM sbtscala/scala-sbt:graalvm-ce-22.3.3-b1-java17_1.10.11_3.6.4 AS sbt
WORKDIR /build
COPY project /build/project
COPY build.sbt /build/
COPY src /build/src
RUN sbt fastLinkJS

FROM node:23-alpine3.20 AS node
WORKDIR /build
COPY package.json package-lock.json /build/
RUN npm ci --ignore-scripts

FROM node AS node-builder
COPY --from=sbt /build/target /build/target
COPY --from=sbt /build/dist /build/dist
COPY vite.config.js /build/
COPY public /build/public
COPY src/index.html src/main.css /build/src/
RUN npm run build

FROM nginx:alpine
COPY --from=node-builder /build/dist /dist
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
