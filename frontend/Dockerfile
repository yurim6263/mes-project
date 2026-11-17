# 개발 서버(Vite)를 컨테이너로 띄운다. 배포용(nginx) 빌드는 필요해지면 추가.
FROM node:24-alpine
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
EXPOSE 5173
CMD ["npm", "run", "dev"]
