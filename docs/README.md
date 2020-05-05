# Roadmap

SAPS is currently composed of four main services: `Archiver`, `Catalog`, `Dispatcher` and `Scheduler`. In addition, it is necessary for SAPS to use a job execution service. Currently, there is only support for `Arrebol`. Given the context, below we have the correct deployment sequence for Saps components.
1. [Catalog](https://github.com/ufcg-lsd/saps-engine/blob/develop/docs/catalog-install.md)
2. [Archiver](https://github.com/ufcg-lsd/saps-engine/blob/develop/docs/archiver-install.md)
3. [Dispatcher](https://github.com/ufcg-lsd/saps-engine/blob/develop/docs/dispatcher-install.md)
4. Job Execution Service
   * [Arrebol](https://github.com/ufcg-lsd/arrebol/tree/feature/remote-worker-saps/deploy)
5. [Scheduler](https://github.com/ufcg-lsd/saps-engine/blob/develop/docs/scheduler-install.md)
6. [Dashboard](https://github.com/ufcg-lsd/saps-dashboard) (Optional)