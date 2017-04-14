# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]
### Added
- Current bills and resolutions in congress are the starting point of documents
- Crawler of bills and bill text
- Searching of bills by subject

### Changed
- node visualization is now a D3 tree layout
- new nodes cannot be added to a document, current nodes can only be changed
- voting on nodes has been removed for now, but will be re-implemented in time
- api to a versioned api based on [JSON:API](http://jsonapi.org/) specification

## [0.0.1] - 2017-03-12
### Added
- Node tree with nodes, versions, comments, and voting
- Document search
- Custom data visualizations based on D3.js
- 'The Park' for temporary user rants
- Crawler for maintaining a database of legislators
