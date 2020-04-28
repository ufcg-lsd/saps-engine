# Roadmap

SAPS is currently composed of four main services: `Archiver`, `Catalog`, `Dispatcher` and `Scheduler`. In addition, saps uses a job execution service called `Arrebol`. Given the context, below we have the correct deployment sequence for Saps components.
1. [Catalog](https://github.com/ufcg-lsd/saps-engine/blob/develop/docs/catalog-install.md)
2. [Archiver](https://github.com/ufcg-lsd/saps-engine/blob/develop/docs/archiver-install.md)
3. [Dispatcher](https://github.com/ufcg-lsd/saps-engine/blob/develop/docs/dispatcher-install.md)
4. [Worker](https://github.com/ufcg-lsd/saps-engine/blob/develop/docs/arrebol-worker.md)
5. [Arrebol](https://github.com/ufcg-lsd/arrebol/tree/feature/remote-worker-saps/deploy)
6. [Scheduler](https://github.com/ufcg-lsd/saps-engine/blob/develop/docs/scheduler-install.md)
7. [Dashboard](https://github.com/ufcg-lsd/saps-dashboard) (Optional)