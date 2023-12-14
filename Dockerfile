# FROM node:19.7.0-alpine as bundle-frontend
# WORKDIR /app
# COPY \
#   package.json \
#   package-lock.json \
#   .babelrc \
#   webpack.config.js \
#   ./
# COPY frontend/ ./frontend
# RUN npm ci
# RUN npm run build


FROM python:3.11-slim-buster

ENV PYTHONUNBUFFERED 1

RUN apt-get update \
  && apt-get install -y \
  # build dependencies
  build-essential pkg-config \
  # psycopg2 dependencies
  libpq-dev \
  # Translations dependencies
  gettext \
  # XML packages
  libxml2-dev libxmlsec1-dev libxmlsec1-openssl \
  # Graphviz
  python-pydot python-pydot-ng graphviz \
  # Gis
  binutils libproj-dev gdal-bin \
  # Git
  git \
  # cleaning up unused files
  && apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false \
  && rm -rf /var/lib/apt/lists/*

# Requirements are installed here to ensure they will be cached.
RUN addgroup --system django \
  && adduser --system --ingroup django django

COPY pyproject.toml poetry.lock .github/workflows/constraints.txt /app/
WORKDIR /app

RUN ls
RUN ls ./github/workflows

# Requirements are installed here to ensure they will be cached.
RUN pip install --constraint=./.github/workflows/constraints.txt poetry
RUN poetry config virtualenvs.create false
RUN poetry install \
  # skip installing project source to avoid cache invalidation on project file change
  --no-root --no-directory

RUN python --version

# COPY ./compose/local/django/celery/worker/start /start-celeryworker
# RUN sed -i 's/\r$//g' /start-celeryworker
# RUN chmod +x /start-celeryworker

# COPY ./compose/local/django/celery/beat/start /start-celerybeat
# RUN sed -i 's/\r$//g' /start-celerybeat
# RUN chmod +x /start-celerybeat

# COPY ./compose/local/django/celery/flower/start /start-flower
# RUN sed -i 's/\r$//g' /start-flower
# RUN chmod +x /start-flower

# Copy over directory containing bundled react app from previous stage
# COPY --from=bundle-frontend /app/frontend/dist ./frontend/dist
