# CodeOps-Courier Module Audit

**Generated:** 2026-02-27
**Source Path:** `src/main/java/com/codeops/courier/`
**API Prefix:** `/api/v1/courier` (AppConstants.COURIER_API_PREFIX)

---

## 1. Entities (18 entities + 7 enums)

All entities extend `com.codeops.entity.BaseEntity` (UUID `id`, `Instant createdAt`, `Instant updatedAt`).
All use Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`.

---

### 1.1 Collection
- **File:** `entity/Collection.java`
- **Table:** `collections`
- **Unique Constraints:** `(team_id, name)`
- **Indexes:** `idx_collections_team_id` (team_id), `idx_collections_created_by` (created_by)
- **Fields:**
  | Field | Type | Column | Nullable | Length/Def | Notes |
  |---|---|---|---|---|---|
  | teamId | UUID | team_id | false | | |
  | name | String | name | false | 200 | |
  | description | String | description | true | 2000 | |
  | preRequestScript | String | pre_request_script | true | TEXT | |
  | postResponseScript | String | post_response_script | true | TEXT | |
  | authType | AuthType | auth_type | true | @Enumerated(STRING) | |
  | authConfig | String | auth_config | true | TEXT | JSON auth config |
  | isShared | boolean | is_shared | false | default false | @Builder.Default |
  | createdBy | UUID | created_by | false | | |
- **Relationships:**
  - `@OneToMany(mappedBy = "collection", cascade = ALL, orphanRemoval = true) List<Folder> folders`
  - `@OneToMany(mappedBy = "collection", cascade = ALL, orphanRemoval = true) List<EnvironmentVariable> variables`

---

### 1.2 CollectionShare
- **File:** `entity/CollectionShare.java`
- **Table:** `collection_shares`
- **Unique Constraints:** `(collection_id, shared_with_user_id)`
- **Indexes:** `idx_collection_shares_collection_id` (collection_id), `idx_collection_shares_shared_with` (shared_with_user_id)
- **Fields:**
  | Field | Type | Column | Nullable | Notes |
  |---|---|---|---|---|
  | permission | SharePermission | permission | false | @Enumerated(STRING) |
  | sharedWithUserId | UUID | shared_with_user_id | false | |
  | sharedByUserId | UUID | shared_by_user_id | false | |
- **Relationships:**
  - `@ManyToOne(LAZY) Collection collection` (collection_id, not null)

---

### 1.3 Environment
- **File:** `entity/Environment.java`
- **Table:** `environments`
- **Unique Constraints:** `(team_id, name)`
- **Indexes:** `idx_environments_team_id` (team_id)
- **Fields:**
  | Field | Type | Column | Nullable | Length/Def | Notes |
  |---|---|---|---|---|---|
  | teamId | UUID | team_id | false | | |
  | name | String | name | false | 200 | |
  | description | String | description | true | 2000 | |
  | isActive | boolean | is_active | false | default false | @Builder.Default |
  | createdBy | UUID | created_by | false | | |
- **Relationships:**
  - `@OneToMany(mappedBy = "environment", cascade = ALL, orphanRemoval = true) List<EnvironmentVariable> variables`

---

### 1.4 EnvironmentVariable
- **File:** `entity/EnvironmentVariable.java`
- **Table:** `environment_variables`
- **Indexes:** `idx_env_variables_environment_id` (environment_id), `idx_env_variables_collection_id` (collection_id)
- **Fields:**
  | Field | Type | Column | Nullable | Length/Def | Notes |
  |---|---|---|---|---|---|
  | variableKey | String | variable_key | false | 500 | |
  | variableValue | String | variable_value | true | 5000 | |
  | isSecret | boolean | is_secret | false | default false | @Builder.Default |
  | isEnabled | boolean | is_enabled | false | default true | @Builder.Default |
  | scope | String | scope | false | 20 | e.g. "ENVIRONMENT", "COLLECTION" |
- **Relationships:**
  - `@ManyToOne(LAZY) Environment environment` (environment_id, nullable)
  - `@ManyToOne(LAZY) Collection collection` (collection_id, nullable)

---

### 1.5 Folder
- **File:** `entity/Folder.java`
- **Table:** `folders`
- **Indexes:** `idx_folders_collection_id` (collection_id), `idx_folders_parent_folder_id` (parent_folder_id)
- **Fields:**
  | Field | Type | Column | Nullable | Length/Def | Notes |
  |---|---|---|---|---|---|
  | name | String | name | false | 200 | |
  | description | String | description | true | 2000 | |
  | sortOrder | int | sort_order | false | default 0 | @Builder.Default |
  | preRequestScript | String | pre_request_script | true | TEXT | |
  | postResponseScript | String | post_response_script | true | TEXT | |
  | authType | AuthType | auth_type | true | @Enumerated(STRING) | |
  | authConfig | String | auth_config | true | TEXT | |
- **Relationships:**
  - `@ManyToOne(LAZY) Collection collection` (collection_id, not null)
  - `@ManyToOne(LAZY) Folder parentFolder` (parent_folder_id, nullable) -- self-referential
  - `@OneToMany(mappedBy = "parentFolder", cascade = ALL, orphanRemoval = true) List<Folder> subFolders`
  - `@OneToMany(mappedBy = "folder", cascade = ALL, orphanRemoval = true) List<Request> requests`

---

### 1.6 Fork
- **File:** `entity/Fork.java`
- **Table:** `forks`
- **Indexes:** `idx_forks_source_collection_id` (source_collection_id), `idx_forks_forked_by_user_id` (forked_by_user_id)
- **Fields:**
  | Field | Type | Column | Nullable | Notes |
  |---|---|---|---|---|
  | forkedByUserId | UUID | forked_by_user_id | false | |
  | forkedAt | Instant | forked_at | false | |
  | label | String | label | true | 200 |
- **Relationships:**
  - `@ManyToOne(LAZY) Collection sourceCollection` (source_collection_id, not null)
  - `@OneToOne(LAZY) Collection forkedCollection` (forked_collection_id, not null, unique)

---

### 1.7 GlobalVariable
- **File:** `entity/GlobalVariable.java`
- **Table:** `global_variables`
- **Unique Constraints:** `(team_id, variable_key)`
- **Indexes:** `idx_global_variables_team_id` (team_id)
- **Fields:**
  | Field | Type | Column | Nullable | Length/Def | Notes |
  |---|---|---|---|---|---|
  | teamId | UUID | team_id | false | | |
  | variableKey | String | variable_key | false | 500 | |
  | variableValue | String | variable_value | true | 5000 | |
  | isSecret | boolean | is_secret | false | default false | @Builder.Default |
  | isEnabled | boolean | is_enabled | false | default true | @Builder.Default |

---

### 1.8 MergeRequest
- **File:** `entity/MergeRequest.java`
- **Table:** `merge_requests`
- **Indexes:** `idx_merge_requests_source_fork_id` (source_fork_id), `idx_merge_requests_target_collection_id` (target_collection_id), `idx_merge_requests_status` (status)
- **Fields:**
  | Field | Type | Column | Nullable | Length | Notes |
  |---|---|---|---|---|---|
  | title | String | title | false | 200 | |
  | description | String | description | true | 5000 | |
  | status | String | status | false | 20 | "OPEN", "MERGED", "CLOSED" |
  | requestedByUserId | UUID | requested_by_user_id | false | | |
  | reviewedByUserId | UUID | reviewed_by_user_id | true | | |
  | mergedAt | Instant | merged_at | true | | |
  | conflictDetails | String | conflict_details | true | TEXT | |
- **Relationships:**
  - `@ManyToOne(LAZY) Fork sourceFork` (source_fork_id, not null)
  - `@ManyToOne(LAZY) Collection targetCollection` (target_collection_id, not null)

---

### 1.9 Request
- **File:** `entity/Request.java`
- **Table:** `requests`
- **Indexes:** `idx_requests_folder_id` (folder_id)
- **Fields:**
  | Field | Type | Column | Nullable | Length/Def | Notes |
  |---|---|---|---|---|---|
  | name | String | name | false | 200 | |
  | description | String | description | true | 2000 | |
  | method | HttpMethod | method | false | @Enumerated(STRING) | |
  | url | String | url | false | 2000 | |
  | sortOrder | int | sort_order | false | default 0 | @Builder.Default |
- **Relationships:**
  - `@ManyToOne(LAZY) Folder folder` (folder_id, not null)
  - `@OneToMany(mappedBy = "request", cascade = ALL, orphanRemoval = true) List<RequestHeader> headers`
  - `@OneToMany(mappedBy = "request", cascade = ALL, orphanRemoval = true) List<RequestParam> params`
  - `@OneToOne(mappedBy = "request", cascade = ALL, orphanRemoval = true) RequestBody body`
  - `@OneToOne(mappedBy = "request", cascade = ALL, orphanRemoval = true) RequestAuth auth`
  - `@OneToMany(mappedBy = "request", cascade = ALL, orphanRemoval = true) List<RequestScript> scripts`

---

### 1.10 RequestAuth
- **File:** `entity/RequestAuth.java`
- **Table:** `request_auths`
- **Fields:**
  | Field | Type | Column | Nullable | Length | Notes |
  |---|---|---|---|---|---|
  | authType | AuthType | auth_type | false | @Enumerated(STRING) | |
  | apiKeyHeader | String | api_key_header | true | 200 | |
  | apiKeyValue | String | api_key_value | true | 2000 | |
  | apiKeyAddTo | String | api_key_add_to | true | 20 | "header" or "query" |
  | bearerToken | String | bearer_token | true | 5000 | |
  | basicUsername | String | basic_username | true | 500 | |
  | basicPassword | String | basic_password | true | 500 | |
  | oauth2GrantType | String | oauth2_grant_type | true | 50 | |
  | oauth2AuthUrl | String | oauth2_auth_url | true | 2000 | |
  | oauth2TokenUrl | String | oauth2_token_url | true | 2000 | |
  | oauth2ClientId | String | oauth2_client_id | true | 500 | |
  | oauth2ClientSecret | String | oauth2_client_secret | true | 500 | |
  | oauth2Scope | String | oauth2_scope | true | 1000 | |
  | oauth2CallbackUrl | String | oauth2_callback_url | true | 2000 | |
  | oauth2AccessToken | String | oauth2_access_token | true | 5000 | |
  | jwtSecret | String | jwt_secret | true | 2000 | |
  | jwtPayload | String | jwt_payload | true | TEXT | |
  | jwtAlgorithm | String | jwt_algorithm | true | 20 | |
- **Relationships:**
  - `@OneToOne(LAZY) Request request` (request_id, not null, unique)

---

### 1.11 RequestBody
- **File:** `entity/RequestBody.java`
- **Table:** `request_bodies`
- **Fields:**
  | Field | Type | Column | Nullable | Notes |
  |---|---|---|---|---|
  | bodyType | BodyType | body_type | false | @Enumerated(STRING) |
  | rawContent | String | raw_content | true | TEXT |
  | formData | String | form_data | true | TEXT |
  | graphqlQuery | String | graphql_query | true | TEXT |
  | graphqlVariables | String | graphql_variables | true | TEXT |
  | binaryFileName | String | binary_file_name | true | 500 |
- **Relationships:**
  - `@OneToOne(LAZY) Request request` (request_id, not null, unique)

---

### 1.12 RequestHeader
- **File:** `entity/RequestHeader.java`
- **Table:** `request_headers`
- **Indexes:** `idx_request_headers_request_id` (request_id)
- **Fields:**
  | Field | Type | Column | Nullable | Length/Def | Notes |
  |---|---|---|---|---|---|
  | headerKey | String | header_key | false | 500 | |
  | headerValue | String | header_value | true | 5000 | |
  | description | String | description | true | 500 | |
  | isEnabled | boolean | is_enabled | false | default true | @Builder.Default |
- **Relationships:**
  - `@ManyToOne(LAZY) Request request` (request_id, not null)

---

### 1.13 RequestHistory
- **File:** `entity/RequestHistory.java`
- **Table:** `request_history`
- **Indexes:** `idx_request_history_team_id` (team_id), `idx_request_history_user_id` (user_id), `idx_request_history_request_method` (request_method), `idx_request_history_created_at` (created_at)
- **Fields:**
  | Field | Type | Column | Nullable | Length | Notes |
  |---|---|---|---|---|---|
  | teamId | UUID | team_id | false | | |
  | userId | UUID | user_id | false | | |
  | requestMethod | HttpMethod | request_method | false | @Enumerated(STRING) | |
  | requestUrl | String | request_url | false | 2000 | |
  | requestHeaders | String | request_headers | true | TEXT | JSON |
  | requestBody | String | request_body | true | TEXT | |
  | responseStatus | Integer | response_status | true | | |
  | responseHeaders | String | response_headers | true | TEXT | JSON |
  | responseBody | String | response_body | true | TEXT | |
  | responseSizeBytes | Long | response_size_bytes | true | | |
  | responseTimeMs | Long | response_time_ms | true | | |
  | contentType | String | content_type | true | 200 | |
  | collectionId | UUID | collection_id | true | | Denormalized FK |
  | requestId | UUID | request_id | true | | Denormalized FK |
  | environmentId | UUID | environment_id | true | | Denormalized FK |
- **Relationships:** None (standalone entity, denormalized references)

---

### 1.14 RequestParam
- **File:** `entity/RequestParam.java`
- **Table:** `request_params`
- **Indexes:** `idx_request_params_request_id` (request_id)
- **Fields:**
  | Field | Type | Column | Nullable | Length/Def | Notes |
  |---|---|---|---|---|---|
  | paramKey | String | param_key | false | 500 | |
  | paramValue | String | param_value | true | 5000 | |
  | description | String | description | true | 500 | |
  | isEnabled | boolean | is_enabled | false | default true | @Builder.Default |
- **Relationships:**
  - `@ManyToOne(LAZY) Request request` (request_id, not null)

---

### 1.15 RequestScript
- **File:** `entity/RequestScript.java`
- **Table:** `request_scripts`
- **Unique Constraints:** `(request_id, script_type)`
- **Indexes:** `idx_request_scripts_request_id` (request_id)
- **Fields:**
  | Field | Type | Column | Nullable | Notes |
  |---|---|---|---|---|
  | scriptType | ScriptType | script_type | false | @Enumerated(STRING) |
  | content | String | content | true | TEXT |
- **Relationships:**
  - `@ManyToOne(LAZY) Request request` (request_id, not null)

---

### 1.16 RunIteration
- **File:** `entity/RunIteration.java`
- **Table:** `run_iterations`
- **Indexes:** `idx_run_iterations_run_result_id` (run_result_id)
- **Fields:**
  | Field | Type | Column | Nullable | Length/Def | Notes |
  |---|---|---|---|---|---|
  | iterationNumber | int | iteration_number | false | | |
  | requestName | String | request_name | false | 200 | |
  | requestMethod | HttpMethod | request_method | false | @Enumerated(STRING) | |
  | requestUrl | String | request_url | false | 2000 | |
  | responseStatus | Integer | response_status | true | | |
  | responseTimeMs | Long | response_time_ms | true | | |
  | responseSizeBytes | Long | response_size_bytes | true | | |
  | passed | boolean | passed | false | default true | @Builder.Default |
  | assertionResults | String | assertion_results | true | TEXT | JSON array |
  | errorMessage | String | error_message | true | 5000 | |
  | requestData | String | request_data | true | TEXT | JSON |
  | responseData | String | response_data | true | TEXT | JSON |
- **Relationships:**
  - `@ManyToOne(LAZY) RunResult runResult` (run_result_id, not null)

---

### 1.17 RunResult
- **File:** `entity/RunResult.java`
- **Table:** `run_results`
- **Indexes:** `idx_run_results_team_id` (team_id), `idx_run_results_collection_id` (collection_id), `idx_run_results_status` (status)
- **Fields:**
  | Field | Type | Column | Nullable | Length/Def | Notes |
  |---|---|---|---|---|---|
  | teamId | UUID | team_id | false | | |
  | collectionId | UUID | collection_id | false | | Denormalized FK |
  | environmentId | UUID | environment_id | true | | Denormalized FK |
  | status | RunStatus | status | false | @Enumerated(STRING) | |
  | totalRequests | int | total_requests | false | default 0 | @Builder.Default |
  | passedRequests | int | passed_requests | false | default 0 | @Builder.Default |
  | failedRequests | int | failed_requests | false | default 0 | @Builder.Default |
  | totalAssertions | int | total_assertions | false | default 0 | @Builder.Default |
  | passedAssertions | int | passed_assertions | false | default 0 | @Builder.Default |
  | failedAssertions | int | failed_assertions | false | default 0 | @Builder.Default |
  | totalDurationMs | long | total_duration_ms | false | default 0 | @Builder.Default |
  | iterationCount | int | iteration_count | false | default 1 | @Builder.Default |
  | delayBetweenRequestsMs | int | delay_between_requests_ms | false | default 0 | @Builder.Default |
  | dataFilename | String | data_filename | true | 500 | |
  | startedAt | Instant | started_at | false | | |
  | completedAt | Instant | completed_at | true | | |
  | startedByUserId | UUID | started_by_user_id | false | | |
- **Relationships:**
  - `@OneToMany(mappedBy = "runResult", cascade = ALL, orphanRemoval = true) List<RunIteration> iterations`

---

### 1.18 CodeSnippetTemplate
- **File:** `entity/CodeSnippetTemplate.java`
- **Table:** `code_snippet_templates`
- **Fields:**
  | Field | Type | Column | Nullable | Length | Notes |
  |---|---|---|---|---|---|
  | language | CodeLanguage | language | false | unique | @Enumerated(STRING) |
  | displayName | String | display_name | false | 100 | |
  | templateContent | String | template_content | false | TEXT | Mustache-style {{placeholder}} |
  | fileExtension | String | file_extension | false | 20 | |
  | contentType | String | content_type | true | 100 | |

---

### 1.19 Enums (7)

| Enum | File | Values |
|---|---|---|
| AuthType | `entity/enums/AuthType.java` | NO_AUTH, API_KEY, BEARER_TOKEN, BASIC_AUTH, OAUTH2_AUTHORIZATION_CODE, OAUTH2_CLIENT_CREDENTIALS, OAUTH2_IMPLICIT, OAUTH2_PASSWORD, JWT_BEARER, INHERIT_FROM_PARENT |
| BodyType | `entity/enums/BodyType.java` | NONE, FORM_DATA, X_WWW_FORM_URLENCODED, RAW_JSON, RAW_XML, RAW_HTML, RAW_TEXT, RAW_YAML, BINARY, GRAPHQL |
| CodeLanguage | `entity/enums/CodeLanguage.java` | CURL, PYTHON_REQUESTS, JAVASCRIPT_FETCH, JAVASCRIPT_AXIOS, JAVA_HTTP_CLIENT, JAVA_OKHTTP, CSHARP_HTTP_CLIENT, GO, RUBY, PHP, SWIFT, KOTLIN |
| HttpMethod | `entity/enums/HttpMethod.java` | GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS |
| RunStatus | `entity/enums/RunStatus.java` | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED |
| ScriptType | `entity/enums/ScriptType.java` | PRE_REQUEST, POST_RESPONSE |
| SharePermission | `entity/enums/SharePermission.java` | VIEWER, EDITOR, ADMIN |

---

## 2. Repositories (18)

All repositories are in `com.codeops.courier.repository`, annotated `@Repository`, extend `JpaRepository<Entity, UUID>`.

---

### 2.1 CollectionRepository
- **Entity:** Collection
- **Custom Methods:**
  - `List<Collection> findByTeamId(UUID teamId)`
  - `Optional<Collection> findByTeamIdAndName(UUID teamId, String name)`
  - `List<Collection> findByTeamIdAndIsSharedTrue(UUID teamId)`
  - `List<Collection> findByCreatedBy(UUID userId)`
  - `boolean existsByTeamIdAndName(UUID teamId, String name)`
  - `long countByTeamId(UUID teamId)`
  - `Page<Collection> findByTeamId(UUID teamId, Pageable pageable)`
  - `List<Collection> findByTeamIdAndNameContainingIgnoreCase(UUID teamId, String name)`

### 2.2 CollectionShareRepository
- **Entity:** CollectionShare
- **Custom Methods:**
  - `List<CollectionShare> findByCollectionId(UUID collectionId)`
  - `List<CollectionShare> findBySharedWithUserId(UUID userId)`
  - `Optional<CollectionShare> findByCollectionIdAndSharedWithUserId(UUID collectionId, UUID userId)`
  - `boolean existsByCollectionIdAndSharedWithUserId(UUID collectionId, UUID userId)`
  - `void deleteByCollectionIdAndSharedWithUserId(UUID collectionId, UUID userId)`

### 2.3 EnvironmentRepository
- **Entity:** Environment
- **Custom Methods:**
  - `List<Environment> findByTeamId(UUID teamId)`
  - `Optional<Environment> findByTeamIdAndName(UUID teamId, String name)`
  - `Optional<Environment> findByTeamIdAndIsActiveTrue(UUID teamId)`
  - `boolean existsByTeamIdAndName(UUID teamId, String name)`
  - `long countByTeamId(UUID teamId)`

### 2.4 EnvironmentVariableRepository
- **Entity:** EnvironmentVariable
- **Custom Methods:**
  - `List<EnvironmentVariable> findByEnvironmentId(UUID environmentId)`
  - `List<EnvironmentVariable> findByCollectionId(UUID collectionId)`
  - `List<EnvironmentVariable> findByEnvironmentIdAndIsEnabledTrue(UUID environmentId)`
  - `List<EnvironmentVariable> findByCollectionIdAndIsEnabledTrue(UUID collectionId)`
  - `void deleteByEnvironmentId(UUID environmentId)`

### 2.5 FolderRepository
- **Entity:** Folder
- **Custom Methods:**
  - `List<Folder> findByCollectionIdOrderBySortOrder(UUID collectionId)`
  - `List<Folder> findByParentFolderIdOrderBySortOrder(UUID parentFolderId)`
  - `List<Folder> findByCollectionIdAndParentFolderIsNullOrderBySortOrder(UUID collectionId)`
  - `long countByCollectionId(UUID collectionId)`
  - `long countByParentFolderId(UUID parentFolderId)`

### 2.6 ForkRepository
- **Entity:** Fork
- **Custom Methods:**
  - `List<Fork> findBySourceCollectionId(UUID collectionId)`
  - `Optional<Fork> findByForkedCollectionId(UUID collectionId)`
  - `List<Fork> findByForkedByUserId(UUID userId)`
  - `boolean existsBySourceCollectionIdAndForkedByUserId(UUID collectionId, UUID userId)`

### 2.7 GlobalVariableRepository
- **Entity:** GlobalVariable
- **Custom Methods:**
  - `List<GlobalVariable> findByTeamId(UUID teamId)`
  - `List<GlobalVariable> findByTeamIdAndIsEnabledTrue(UUID teamId)`
  - `Optional<GlobalVariable> findByTeamIdAndVariableKey(UUID teamId, String variableKey)`
  - `boolean existsByTeamIdAndVariableKey(UUID teamId, String variableKey)`

### 2.8 MergeRequestRepository
- **Entity:** MergeRequest
- **Custom Methods:**
  - `List<MergeRequest> findByTargetCollectionId(UUID collectionId)`
  - `List<MergeRequest> findBySourceForkId(UUID forkId)`
  - `List<MergeRequest> findByTargetCollectionIdAndStatus(UUID collectionId, String status)`
  - `List<MergeRequest> findByRequestedByUserId(UUID userId)`

### 2.9 RequestRepository
- **Entity:** Request
- **Custom Methods:**
  - `List<Request> findByFolderIdOrderBySortOrder(UUID folderId)`
  - `long countByFolderId(UUID folderId)`

### 2.10 RequestAuthRepository
- **Entity:** RequestAuth
- **Custom Methods:**
  - `Optional<RequestAuth> findByRequestId(UUID requestId)`
  - `void deleteByRequestId(UUID requestId)`

### 2.11 RequestBodyRepository
- **Entity:** RequestBody
- **Custom Methods:**
  - `Optional<RequestBody> findByRequestId(UUID requestId)`
  - `void deleteByRequestId(UUID requestId)`

### 2.12 RequestHeaderRepository
- **Entity:** RequestHeader
- **Custom Methods:**
  - `List<RequestHeader> findByRequestId(UUID requestId)`
  - `void deleteByRequestId(UUID requestId)`

### 2.13 RequestHistoryRepository
- **Entity:** RequestHistory
- **Custom Methods:**
  - `Page<RequestHistory> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable)`
  - `Page<RequestHistory> findByTeamIdAndUserId(UUID teamId, UUID userId, Pageable pageable)`
  - `Page<RequestHistory> findByTeamIdAndRequestMethod(UUID teamId, HttpMethod method, Pageable pageable)`
  - `List<RequestHistory> findByTeamIdAndRequestUrlContainingIgnoreCase(UUID teamId, String urlFragment)`
  - `void deleteByTeamIdAndCreatedAtBefore(UUID teamId, Instant cutoff)`
  - `void deleteByTeamId(UUID teamId)`
  - `long countByTeamId(UUID teamId)`
  - `List<RequestHistory> findByTeamIdAndCreatedAtBefore(UUID teamId, Instant cutoff)`

### 2.14 RequestParamRepository
- **Entity:** RequestParam
- **Custom Methods:**
  - `List<RequestParam> findByRequestId(UUID requestId)`
  - `void deleteByRequestId(UUID requestId)`

### 2.15 RequestScriptRepository
- **Entity:** RequestScript
- **Custom Methods:**
  - `List<RequestScript> findByRequestId(UUID requestId)`
  - `Optional<RequestScript> findByRequestIdAndScriptType(UUID requestId, ScriptType scriptType)`
  - `void deleteByRequestId(UUID requestId)`

### 2.16 RunResultRepository
- **Entity:** RunResult
- **Custom Methods:**
  - `List<RunResult> findByTeamIdOrderByCreatedAtDesc(UUID teamId)`
  - `List<RunResult> findByCollectionIdOrderByCreatedAtDesc(UUID collectionId)`
  - `Page<RunResult> findByTeamId(UUID teamId, Pageable pageable)`
  - `List<RunResult> findByStatus(RunStatus status)`

### 2.17 RunIterationRepository
- **Entity:** RunIteration
- **Custom Methods:**
  - `List<RunIteration> findByRunResultIdOrderByIterationNumber(UUID runResultId)`

### 2.18 CodeSnippetTemplateRepository
- **Entity:** CodeSnippetTemplate
- **Custom Methods:**
  - `Optional<CodeSnippetTemplate> findByLanguage(CodeLanguage language)`
  - `List<CodeSnippetTemplate> findAllByOrderByDisplayNameAsc()`
  - `boolean existsByLanguage(CodeLanguage language)`

---

## 3. Services (22)

All services are in `com.codeops.courier.service`.
Convention: `@Service @Slf4j @RequiredArgsConstructor @Transactional` (class-level), `@Transactional(readOnly = true)` on read methods.

---

### 3.1 CollectionService
- **File:** `service/CollectionService.java`
- **Dependencies:** CollectionRepository, FolderRepository, RequestRepository, CollectionShareRepository, CollectionMapper
- **Public Methods:**
  | Method | Return | Purpose | Throws | Txn |
  |---|---|---|---|---|
  | `createCollection(UUID teamId, UUID userId, CreateCollectionRequest)` | CollectionResponse | Creates collection, validates name uniqueness | ValidationException | write |
  | `getCollection(UUID collectionId, UUID teamId)` | CollectionResponse | Gets single collection with counts | NotFoundException | readOnly |
  | `getCollections(UUID teamId, UUID userId)` | List\<CollectionSummaryResponse\> | Lists owned + shared collections | -- | readOnly |
  | `getCollectionsPaged(UUID teamId, int page, int size)` | PageResponse\<CollectionSummaryResponse\> | Paginated listing | -- | readOnly |
  | `updateCollection(UUID collectionId, UUID teamId, UpdateCollectionRequest)` | CollectionResponse | Partial update, validates name uniqueness | NotFoundException, ValidationException | write |
  | `deleteCollection(UUID collectionId, UUID teamId)` | void | Hard delete with cascades | NotFoundException | write |
  | `duplicateCollection(UUID collectionId, UUID teamId, UUID userId)` | CollectionResponse | Deep copy: folders, requests, components, variables | NotFoundException | write |
  | `searchCollections(UUID teamId, String query)` | List\<CollectionSummaryResponse\> | Case-insensitive name search | -- | readOnly |
- **Package-Private Methods:**
  - `Collection findCollectionByIdAndTeam(UUID collectionId, UUID teamId)` -- used by ForkService, MergeService
- **Private Methods:**
  - `toResponse(Collection, int folderCount, int requestCount)`
  - `enrichResponse(Collection)` -- computes folderCount + requestCount
  - `enrichSummaryResponse(Collection)` -- computes folderCount + requestCount
  - `countTotalRequests(UUID collectionId)` -- sums request counts across all folders
  - `deepCopyFolder(Folder source, Collection target, Folder parent)` -- recursive deep copy
  - `deepCopyRequest(Request source, Folder target)` -- copies request + all components

---

### 3.2 RequestService
- **File:** `service/RequestService.java`
- **Dependencies:** RequestRepository, RequestHeaderRepository, RequestParamRepository, RequestBodyRepository, RequestAuthRepository, RequestScriptRepository, FolderRepository, CollectionRepository, RequestMapper, RequestHeaderMapper, RequestParamMapper, RequestBodyMapper, RequestAuthMapper, RequestScriptMapper (14 dependencies)
- **Public Methods:**
  | Method | Return | Purpose | Throws | Txn |
  |---|---|---|---|---|
  | `createRequest(UUID teamId, CreateRequestRequest)` | RequestResponse | Creates request in folder | NotFoundException | write |
  | `getRequest(UUID requestId, UUID teamId)` | RequestResponse | Gets full request with all components | NotFoundException | readOnly |
  | `getRequestsInFolder(UUID folderId, UUID teamId)` | List\<RequestSummaryResponse\> | Lists requests in folder (summary only) | NotFoundException | readOnly |
  | `updateRequest(UUID requestId, UUID teamId, UpdateRequestRequest)` | RequestResponse | Partial update of request metadata | NotFoundException | write |
  | `deleteRequest(UUID requestId, UUID teamId)` | void | Hard delete with cascades | NotFoundException | write |
  | `duplicateRequest(UUID requestId, UUID teamId, DuplicateRequestRequest)` | RequestResponse | Deep copy with all components | NotFoundException, ValidationException | write |
  | `moveRequest(UUID requestId, UUID teamId, UUID targetFolderId)` | RequestResponse | Moves request to different folder (same collection) | NotFoundException, ValidationException | write |
  | `reorderRequests(UUID teamId, ReorderRequestRequest)` | List\<RequestSummaryResponse\> | Sets sortOrder based on ordered IDs | NotFoundException, ValidationException | write |
  | `saveHeaders(UUID requestId, UUID teamId, SaveRequestHeadersRequest)` | List\<RequestHeaderResponse\> | Batch replace: delete all then save new | NotFoundException | write |
  | `saveParams(UUID requestId, UUID teamId, SaveRequestParamsRequest)` | List\<RequestParamResponse\> | Batch replace: delete all then save new | NotFoundException | write |
  | `saveBody(UUID requestId, UUID teamId, SaveRequestBodyRequest)` | RequestBodyResponse | Replace: delete existing then save new | NotFoundException | write |
  | `saveAuth(UUID requestId, UUID teamId, SaveRequestAuthRequest)` | RequestAuthResponse | Replace: delete existing then save new | NotFoundException | write |
  | `saveScript(UUID requestId, UUID teamId, SaveRequestScriptRequest)` | RequestScriptResponse | Upsert by scriptType | NotFoundException | write |
  | `clearRequestComponents(UUID requestId, UUID teamId)` | void | Deletes all components, keeps request | NotFoundException | write |
- **Private Methods:**
  - `findRequestAndValidateTeam(UUID requestId, UUID teamId)` -- validates via folder -> collection -> teamId chain
  - `findFolderAndValidateTeam(UUID folderId, UUID teamId)` -- validates via collection -> teamId chain
  - `buildFullResponse(Request)` -- assembles all components into RequestResponse
  - `computeNextRequestSortOrder(UUID folderId)` -- max(sortOrder) + 1

---

### 3.3 FolderService
- **File:** `service/FolderService.java`
- **Dependencies:** FolderRepository, CollectionRepository, RequestRepository, FolderMapper, RequestMapper
- **Public Methods:**
  | Method | Return | Purpose | Throws | Txn |
  |---|---|---|---|---|
  | `createFolder(UUID teamId, CreateFolderRequest)` | FolderResponse | Creates folder in collection (root or nested) | NotFoundException, ValidationException | write |
  | `getFolder(UUID folderId, UUID teamId)` | FolderResponse | Gets folder with computed counts | NotFoundException | readOnly |
  | `getFolderTree(UUID collectionId, UUID teamId)` | List\<FolderTreeResponse\> | Recursive tree: folders + request summaries | NotFoundException | readOnly |
  | `getSubFolders(UUID parentFolderId, UUID teamId)` | List\<FolderResponse\> | Direct children of a parent folder | NotFoundException | readOnly |
  | `getRootFolders(UUID collectionId, UUID teamId)` | List\<FolderResponse\> | Root-level folders (parentFolder=null) | NotFoundException | readOnly |
  | `updateFolder(UUID folderId, UUID teamId, UpdateFolderRequest)` | FolderResponse | Partial update with circular reference check | NotFoundException, ValidationException | write |
  | `deleteFolder(UUID folderId, UUID teamId)` | void | Hard delete with cascades | NotFoundException | write |
  | `reorderFolders(UUID teamId, ReorderFolderRequest)` | List\<FolderResponse\> | Sets sortOrder based on ordered IDs | NotFoundException, ValidationException | write |
  | `moveFolder(UUID folderId, UUID teamId, UUID newParentFolderId)` | FolderResponse | Moves folder (null = root), circular ref check | NotFoundException, ValidationException | write |
- **Package-Private Methods:**
  - `Folder findFolderAndValidateTeam(UUID folderId, UUID teamId)`
- **Private Methods:**
  - `isDescendant(UUID sourceFolderId, UUID targetFolderId)` -- walks parent chain to detect cycles
  - `findAndValidateCollection(UUID collectionId, UUID teamId)`
  - `toFolderResponse(Folder)` -- includes computed subFolderCount and requestCount
  - `buildTreeNode(Folder)` -- recursive tree builder
  - `computeNextFolderSortOrder(UUID collectionId, UUID parentFolderId)`

---

### 3.4 EnvironmentService
- **File:** `service/EnvironmentService.java`
- **Dependencies:** EnvironmentRepository, EnvironmentVariableRepository, EnvironmentMapper, EnvironmentVariableMapper
- **Constants:** `SECRET_MASK = "--------"` (8 bullet characters)
- **Public Methods:**
  | Method | Return | Purpose | Throws | Txn |
  |---|---|---|---|---|
  | `createEnvironment(UUID teamId, UUID userId, CreateEnvironmentRequest)` | EnvironmentResponse | Creates env; auto-activates if first | ValidationException | write |
  | `getEnvironment(UUID environmentId, UUID teamId)` | EnvironmentResponse | Gets env with variable count | NotFoundException | readOnly |
  | `getEnvironments(UUID teamId)` | List\<EnvironmentResponse\> | Lists all team environments | -- | readOnly |
  | `getActiveEnvironment(UUID teamId)` | EnvironmentResponse | Gets the active environment | NotFoundException | readOnly |
  | `setActiveEnvironment(UUID environmentId, UUID teamId)` | EnvironmentResponse | Deactivates current, activates target | NotFoundException | write |
  | `updateEnvironment(UUID environmentId, UUID teamId, UpdateEnvironmentRequest)` | EnvironmentResponse | Partial update, validates name | NotFoundException, ValidationException | write |
  | `deleteEnvironment(UUID environmentId, UUID teamId)` | void | Deletes; auto-activates next if was active | NotFoundException | write |
  | `cloneEnvironment(UUID environmentId, UUID teamId, UUID userId, CloneEnvironmentRequest)` | EnvironmentResponse | Deep copy with all variables (inactive) | NotFoundException, ValidationException | write |
  | `getEnvironmentVariables(UUID environmentId, UUID teamId)` | List\<EnvironmentVariableResponse\> | Lists vars with secret masking | NotFoundException | readOnly |
  | `saveEnvironmentVariables(UUID environmentId, UUID teamId, SaveEnvironmentVariablesRequest)` | List\<EnvironmentVariableResponse\> | Batch replace all variables | NotFoundException | write |
- **Package-Private Methods:**
  - `Environment findEnvironmentAndValidateTeam(UUID environmentId, UUID teamId)`
- **Private Methods:**
  - `toResponse(Environment, int variableCount)`
  - `maskIfSecret(EnvironmentVariableResponse)` -- replaces value with SECRET_MASK if isSecret

---

### 3.5 VariableService
- **File:** `service/VariableService.java`
- **Dependencies:** GlobalVariableRepository, EnvironmentVariableRepository, GlobalVariableMapper
- **Constants:** `SECRET_MASK`, `VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}")`
- **Public Methods:**
  | Method | Return | Purpose | Throws | Txn |
  |---|---|---|---|---|
  | `resolveVariables(String input, UUID teamId, UUID collectionId, UUID environmentId, Map<String,String> localVars)` | String | Core resolution: replaces {{var}} placeholders with scope hierarchy | -- | readOnly |
  | `buildVariableMap(UUID teamId, UUID collectionId, UUID environmentId, Map<String,String> localVars)` | Map\<String,String\> | Builds merged variable map: Global < Collection < Environment < Local | -- | readOnly |
  | `resolveUrl(String url, UUID teamId, UUID collectionId, UUID environmentId, Map<String,String> localVars)` | String | Delegates to resolveVariables | -- | readOnly |
  | `resolveHeaders(List<RequestHeaderResponse> headers, UUID teamId, UUID collectionId, UUID environmentId, Map<String,String> localVars)` | Map\<String,String\> | Resolves enabled header key/value pairs | -- | readOnly |
  | `resolveBody(String body, UUID teamId, UUID collectionId, UUID environmentId, Map<String,String> localVars)` | String | Delegates to resolveVariables | -- | readOnly |
  | `getGlobalVariables(UUID teamId)` | List\<GlobalVariableResponse\> | Lists globals with secret masking | -- | readOnly |
  | `saveGlobalVariable(UUID teamId, SaveGlobalVariableRequest)` | GlobalVariableResponse | Upsert by key | -- | write |
  | `batchSaveGlobalVariables(UUID teamId, BatchSaveGlobalVariablesRequest)` | List\<GlobalVariableResponse\> | Additive batch upsert | -- | write |
  | `deleteGlobalVariable(UUID variableId, UUID teamId)` | void | Hard delete | NotFoundException | write |
  | `getSecretValue(UUID variableId, UUID teamId)` | String | Returns raw unmasked value (for proxy) | NotFoundException | readOnly |

---

### 3.6 HistoryService
- **File:** `service/HistoryService.java`
- **Dependencies:** RequestHistoryRepository, RequestHistoryMapper
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `getHistory(UUID teamId, int page, int size)` | PageResponse\<RequestHistoryResponse\> | Paginated team history |
  | `getUserHistory(UUID teamId, UUID userId, int page, int size)` | PageResponse\<RequestHistoryResponse\> | Paginated user-specific history |
  | `getHistoryDetail(UUID historyId, UUID teamId)` | RequestHistoryDetailResponse | Full detail with headers/bodies |
  | `searchHistory(UUID teamId, String query)` | List\<RequestHistoryResponse\> | URL-based search |
  | `getHistoryByMethod(UUID teamId, HttpMethod method, int page, int size)` | PageResponse\<RequestHistoryResponse\> | Filter by HTTP method |
  | `deleteHistoryEntry(UUID historyId, UUID teamId)` | void | Single entry delete |
  | `clearTeamHistory(UUID teamId)` | void | Delete all team history |
  | `clearOldHistory(UUID teamId, int daysToRetain)` | void | Delete entries older than N days |
  | `getHistoryCount(UUID teamId)` | long | Total count for team |

---

### 3.7 ShareService
- **File:** `service/ShareService.java`
- **Dependencies:** CollectionShareRepository, CollectionRepository
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `shareCollection(UUID collectionId, UUID teamId, UUID sharedByUserId, ShareCollectionRequest)` | CollectionShareResponse | Creates share; validates no self-share, no duplicate |
  | `getCollectionShares(UUID collectionId, UUID teamId)` | List\<CollectionShareResponse\> | Lists all shares for collection |
  | `getSharedWithUser(UUID userId)` | List\<CollectionShareResponse\> | Lists collections shared with user |
  | `updateSharePermission(UUID collectionId, UUID sharedWithUserId, UUID teamId, UpdateSharePermissionRequest)` | CollectionShareResponse | Updates permission level |
  | `revokeShare(UUID collectionId, UUID sharedWithUserId, UUID teamId)` | void | Removes share |
  | `hasPermission(UUID collectionId, UUID userId, SharePermission required)` | boolean | Permission check via ordinal hierarchy; owner always passes |
  | `canView(UUID collectionId, UUID userId)` | boolean | VIEWER or higher |
  | `canEdit(UUID collectionId, UUID userId)` | boolean | EDITOR or higher |
  | `canAdmin(UUID collectionId, UUID userId)` | boolean | ADMIN only |

---

### 3.8 ForkService
- **File:** `service/ForkService.java`
- **Dependencies:** ForkRepository, CollectionService
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `forkCollection(UUID collectionId, UUID teamId, UUID userId, CreateForkRequest)` | ForkResponse | Duplicates collection as fork |
  | `getForksForCollection(UUID collectionId, UUID teamId)` | List\<ForkResponse\> | Lists forks for a source collection |
  | `getUserForks(UUID userId)` | List\<ForkResponse\> | Lists forks by user |
  | `getFork(UUID forkId, UUID teamId)` | ForkResponse | Gets single fork |

---

### 3.9 MergeService
- **File:** `service/MergeService.java`
- **Dependencies:** MergeRequestRepository, ForkRepository, FolderRepository, CollectionService, ObjectMapper
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `createMergeRequest(UUID forkId, UUID teamId, UUID userId, CreateMergeRequest)` | MergeRequestResponse | Creates merge request with conflict detection |
  | `getMergeRequests(UUID collectionId, UUID teamId)` | List\<MergeRequestResponse\> | Lists merge requests for target collection |
  | `getMergeRequest(UUID mergeRequestId, UUID teamId)` | MergeRequestResponse | Gets single merge request |
  | `resolveMergeRequest(UUID mergeRequestId, UUID teamId, UUID userId, ResolveMergeRequest)` | MergeRequestResponse | Resolves: MERGE (applies changes) or CLOSE (discards) |
- **Inner Record:** `ConflictDetail(String path, String sourceValue, String targetValue)`
- **Private Methods:** `detectConflicts(...)`, `hasContentDifference(...)`, `mergeForkIntoTarget(...)`

---

### 3.10 ImportService
- **File:** `service/ImportService.java`
- **Dependencies:** CollectionRepository, PostmanImporter, OpenApiImporter, CurlImporter
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `importCollection(UUID teamId, UUID userId, ImportCollectionRequest)` | ImportResultResponse | Routes to format-specific importer, persists result |
- **Package-Private Methods:**
  - `String resolveFormat(ImportCollectionRequest)` -- auto-detects format from content
  - `String ensureUniqueName(UUID teamId, String baseName)` -- appends suffix if name exists

---

### 3.11 ExportService
- **File:** `service/ExportService.java`
- **Dependencies:** CollectionService, FolderRepository, ObjectMapper
- **Class-Level:** `@Transactional(readOnly = true)`
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `exportAsPostman(UUID collectionId, UUID teamId)` | ExportCollectionResponse | Postman Collection v2.1 JSON format |
  | `exportAsOpenApi(UUID collectionId, UUID teamId)` | ExportCollectionResponse | OpenAPI 3.0.3 YAML format |
  | `exportAsNative(UUID collectionId, UUID teamId)` | ExportCollectionResponse | CodeOps native JSON format |
- **Private Methods:** Multiple helpers for building Postman item tree, request body, auth blocks, event scripts, and OpenAPI path/operation structures.

---

### 3.12 RequestProxyService
- **File:** `service/RequestProxyService.java`
- **Dependencies:** HttpClient (courierHttpClient), VariableService, AuthResolverService, RequestHistoryRepository, RequestRepository, RequestAuthRepository, ObjectMapper
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `executeRequest(SendRequestProxyRequest, UUID teamId, UUID userId)` | ProxyResponse | Executes ad-hoc HTTP request with variable resolution |
  | `executeStoredRequest(UUID requestId, UUID teamId, UUID userId, UUID environmentId)` | ProxyResponse | Executes stored request with auth inheritance |
- **Package-Private Methods:**
  - `RequestAuth resolveInheritedAuth(Request)` -- walks folder chain then collection for INHERIT_FROM_PARENT
  - `String getStatusText(int statusCode)` -- maps HTTP status codes to text
- **Key Behavior:** Manual redirect tracking (up to COURIER_MAX_REDIRECT_COUNT), history persistence, response body truncation at COURIER_HISTORY_BODY_TRUNCATE_SIZE, auth inheritance chain: request -> folder -> parent folders -> collection.

---

### 3.13 GraphQLService
- **File:** `service/GraphQLService.java`
- **Dependencies:** RequestProxyService, VariableService, ObjectMapper
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `executeQuery(ExecuteGraphQLRequest, UUID teamId, UUID userId)` | GraphQLResponse | Sends GraphQL query via proxy |
  | `introspect(IntrospectGraphQLRequest, UUID teamId, UUID userId)` | GraphQLResponse | Sends full introspection query |
  | `validateQuery(String query)` | List\<String\> | Syntax validation (returns error list) |
  | `formatQuery(String query)` | String | Prettifies GraphQL query |
- **Constants:** Full `INTROSPECTION_QUERY` constant for GraphQL schema introspection.

---

### 3.14 CodeGenerationService
- **File:** `service/CodeGenerationService.java`
- **Dependencies:** CodeSnippetTemplateRepository, RequestService, VariableService
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `generateCode(GenerateCodeRequest, UUID teamId)` | CodeSnippetResponse | Generates code snippet for a request in specified language |
  | `getAvailableLanguages()` | List\<CodeSnippetResponse\> | Returns all supported languages |
  | `getTemplates()` | List\<CodeSnippetTemplateResponse\> | Lists custom templates |
  | `getTemplate(CodeLanguage)` | CodeSnippetTemplateResponse | Gets specific template |
  | `saveTemplate(SaveCodeSnippetTemplateRequest)` | CodeSnippetTemplateResponse | Upserts custom template |
  | `deleteTemplate(CodeLanguage)` | void | Deletes custom template |
- **Built-in generators:** 12 languages (CURL, PYTHON_REQUESTS, JAVASCRIPT_FETCH, JAVASCRIPT_AXIOS, JAVA_HTTP_CLIENT, JAVA_OKHTTP, CSHARP_HTTP_CLIENT, GO, RUBY, PHP, SWIFT, KOTLIN)
- **Template Engine:** `{{placeholder}}` substitution with fallback to built-in generators.
- **Inner Records:** `LanguageMeta(String displayName, String fileExtension, String contentType)`, `ResolvedRequest(String method, String url, Map<String,String> headers, String body)`

---

### 3.15 CollectionRunnerService
- **File:** `service/CollectionRunnerService.java`
- **Dependencies:** RunResultRepository, RunIterationRepository, FolderRepository, RequestRepository, RequestHeaderRepository, RequestParamRepository, RequestBodyRepository, RequestAuthRepository, RequestProxyService, ScriptEngineService, VariableService, DataFileParser, RunResultMapper, ObjectMapper (14 dependencies)
- **Instance State:** `ConcurrentHashMap<UUID, AtomicBoolean> cancelledRuns` -- tracks cancellation flags
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `startRun(StartCollectionRunRequest, UUID teamId, UUID userId)` | RunResultDetailResponse | Executes all requests in collection with iterations, scripts, delay |
  | `getRunResult(UUID runResultId, UUID teamId)` | RunResultResponse | Gets run result summary |
  | `getRunResultDetail(UUID runResultId, UUID teamId)` | RunResultDetailResponse | Gets run with all iterations |
  | `getRunResults(UUID collectionId, UUID teamId)` | List\<RunResultResponse\> | Lists results for collection |
  | `getRunResultsPaged(UUID teamId, int page, int size)` | PageResponse\<RunResultResponse\> | Paginated results for team |
  | `cancelRun(UUID runResultId, UUID teamId, UUID userId)` | RunResultResponse | Sets cancellation flag |
  | `deleteRunResult(UUID runResultId, UUID teamId)` | void | Hard deletes run + iterations |
- **Script Execution Order:**
  - Pre-request: Collection -> Folders (outermost first) -> Request
  - Post-response: Request -> Folders (innermost first) -> Collection

---

### 3.16 AuthResolverService
- **File:** `service/AuthResolverService.java`
- **Dependencies:** VariableService
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `resolveAuth(RequestAuth auth, UUID teamId, UUID collectionId, UUID environmentId, Map<String,String> localVars)` | ResolvedAuth | Resolves auth config to headers + query params |
- **Inner Record:** `ResolvedAuth(Map<String,String> headers, Map<String,String> queryParams)`
- **Supports:** BEARER_TOKEN, BASIC_AUTH (Base64), API_KEY (header or query), OAuth2 variants (bearer token from access_token), JWT_BEARER

---

### 3.17 ScriptEngineService
- **File:** `service/ScriptEngineService.java`
- **No Spring DI Dependencies** (uses GraalJS directly)
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `executePreRequestScript(String script, ScriptContext ctx)` | ScriptContext | Runs pre-request script in GraalJS sandbox |
  | `executePostResponseScript(String script, ScriptContext ctx)` | ScriptContext | Runs post-response script in GraalJS sandbox |
- **Postman-compatible `pm` API:** pm.variables, pm.globals, pm.collectionVariables, pm.environment, pm.request, pm.response, pm.test(), pm.expect()
- **Timeout:** ExecutorService + Future with COURIER_SCRIPT_TIMEOUT_SECONDS

---

### 3.18 ScriptContext
- **File:** `service/ScriptContext.java`
- **Not a Spring Bean** -- mutable POJO for script execution state
- **Fields:**
  - `Map<String,String> variables` (local scope)
  - `Map<String,String> globals`
  - `Map<String,String> collectionVariables`
  - `Map<String,String> environmentVariables`
  - Request data: method, url, headers, body
  - Response data: status, headers, body, responseTime
  - `List<AssertionResult> assertions`
  - `List<String> consoleOutput`
  - `boolean requestCancelled`
- **Inner Record:** `AssertionResult(String name, boolean passed, String errorMessage)`

---

### 3.19 PostmanImporter
- **File:** `service/PostmanImporter.java`
- **Annotation:** `@Component`
- **Dependencies:** ObjectMapper
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `parse(String json, UUID teamId, UUID createdBy)` | PostmanImportResult | Parses Postman Collection v2.1 JSON into entity graph |
- **Inner Record:** `PostmanImportResult(Collection collection, int folderCount, int requestCount)`
- **Supports:** Nested folders, auth mapping (apikey/bearer/basic/oauth2), body mode mapping (raw/urlencoded/formdata/graphql), event scripts (prerequest/test)

---

### 3.20 OpenApiImporter
- **File:** `service/OpenApiImporter.java`
- **Annotation:** `@Component`
- **Dependencies:** ObjectMapper (JSON), ObjectMapper (YAML via YAMLFactory)
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `parse(String content, boolean isYaml, UUID teamId, UUID createdBy)` | OpenApiImportResult | Parses OpenAPI 3.x spec into entity graph |
- **Inner Record:** `OpenApiImportResult(Collection collection, int folderCount, int requestCount)`
- **Supports:** Tag-based folder grouping, security scheme mapping (apiKey, http bearer/basic, oauth2), path parameter extraction, request body examples

---

### 3.21 CurlImporter
- **File:** `service/CurlImporter.java`
- **Annotation:** `@Component`
- **No Spring Dependencies**
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `parseCurl(String curlCommand, Folder folder, int sortOrder)` | Request | Parses cURL command string into Request entity |
- **Package-Private Methods:**
  - `String normalizeCommand(String input)` -- strips line continuations
  - `List<String> tokenize(String input)` -- handles quoted strings
- **Supported Flags:** `-X`, `-H`, `-d`/`--data`, `-u`/`--user`, `-F`/`--form`, `-A`/`--user-agent`

---

### 3.22 DataFileParser
- **File:** `service/DataFileParser.java`
- **Annotation:** `@Service`
- **No Dependencies**
- **Public Methods:**
  | Method | Return | Purpose |
  |---|---|---|
  | `parse(String content, String filename)` | List\<Map\<String,String\>\> | Parses CSV or JSON data files for runner iterations |
- **Supports:** CSV (with quoted field handling) and JSON (array of objects)

---

## 4. Controllers (13)

All controllers in `com.codeops.courier.controller`.
Convention: `@RestController @RequestMapping(AppConstants.COURIER_API_PREFIX + "/...")` `@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")` `@RequiredArgsConstructor @Slf4j @Tag(name = "...")`.
All endpoints use `@RequestHeader("X-Team-ID") UUID teamId` for team scoping and `SecurityUtils.getCurrentUserId()` for user identification (where needed).

---

### 4.1 CollectionController
- **File:** `controller/CollectionController.java`
- **Base Path:** `/api/v1/courier/collections`
- **Dependencies:** CollectionService, ForkService, MergeService, ExportService, FolderService
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | POST | `/` | createCollection | collectionService.createCollection(teamId, userId, req) | 201 |
  | GET | `/` | getCollections | collectionService.getCollections(teamId, userId) | 200 |
  | GET | `/paged` | getCollectionsPaged | collectionService.getCollectionsPaged(teamId, page, size) | 200 |
  | GET | `/search` | searchCollections | collectionService.searchCollections(teamId, query) | 200 |
  | GET | `/{collectionId}` | getCollection | collectionService.getCollection(collectionId, teamId) | 200 |
  | PUT | `/{collectionId}` | updateCollection | collectionService.updateCollection(collectionId, teamId, req) | 200 |
  | DELETE | `/{collectionId}` | deleteCollection | collectionService.deleteCollection(collectionId, teamId) | 204 |
  | POST | `/{collectionId}/duplicate` | duplicateCollection | collectionService.duplicateCollection(collectionId, teamId, userId) | 201 |
  | POST | `/{collectionId}/fork` | forkCollection | forkService.forkCollection(collectionId, teamId, userId, req) | 201 |
  | GET | `/{collectionId}/forks` | getCollectionForks | forkService.getForksForCollection(collectionId, teamId) | 200 |
  | GET | `/{collectionId}/export/{format}` | exportCollection | exportService.exportAs{Postman/OpenApi/Native}(collectionId, teamId) | 200 |
  | GET | `/{collectionId}/tree` | getCollectionTree | folderService.getFolderTree(collectionId, teamId) | 200 |

---

### 4.2 RequestController
- **File:** `controller/RequestController.java`
- **Base Path:** `/api/v1/courier/requests`
- **Dependencies:** RequestService, RequestProxyService
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | POST | `/` | createRequest | requestService.createRequest(teamId, req) | 201 |
  | GET | `/{requestId}` | getRequest | requestService.getRequest(requestId, teamId) | 200 |
  | PUT | `/{requestId}` | updateRequest | requestService.updateRequest(requestId, teamId, req) | 200 |
  | DELETE | `/{requestId}` | deleteRequest | requestService.deleteRequest(requestId, teamId) | 204 |
  | POST | `/{requestId}/duplicate` | duplicateRequest | requestService.duplicateRequest(requestId, teamId, req) | 201 |
  | PUT | `/{requestId}/move` | moveRequest | requestService.moveRequest(requestId, teamId, targetFolderId) | 200 |
  | PUT | `/reorder` | reorderRequests | requestService.reorderRequests(teamId, req) | 200 |
  | PUT | `/{requestId}/headers` | saveHeaders | requestService.saveHeaders(requestId, teamId, req) | 200 |
  | PUT | `/{requestId}/params` | saveParams | requestService.saveParams(requestId, teamId, req) | 200 |
  | PUT | `/{requestId}/body` | saveBody | requestService.saveBody(requestId, teamId, req) | 200 |
  | PUT | `/{requestId}/auth` | saveAuth | requestService.saveAuth(requestId, teamId, req) | 200 |
  | PUT | `/{requestId}/scripts` | saveScript | requestService.saveScript(requestId, teamId, req) | 200 |
  | POST | `/{requestId}/send` | sendRequest | requestProxyService.executeStoredRequest(requestId, teamId, userId, envId) | 200 |

---

### 4.3 FolderController
- **File:** `controller/FolderController.java`
- **Base Path:** `/api/v1/courier/folders`
- **Dependencies:** FolderService, RequestService
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | POST | `/` | createFolder | folderService.createFolder(teamId, req) | 201 |
  | GET | `/{folderId}` | getFolder | folderService.getFolder(folderId, teamId) | 200 |
  | GET | `/{folderId}/subfolders` | getSubFolders | folderService.getSubFolders(folderId, teamId) | 200 |
  | GET | `/{folderId}/requests` | getRequestsInFolder | requestService.getRequestsInFolder(folderId, teamId) | 200 |
  | PUT | `/{folderId}` | updateFolder | folderService.updateFolder(folderId, teamId, req) | 200 |
  | DELETE | `/{folderId}` | deleteFolder | folderService.deleteFolder(folderId, teamId) | 204 |
  | PUT | `/{folderId}/move` | moveFolder | folderService.moveFolder(folderId, teamId, newParentId) | 200 |
  | PUT | `/reorder` | reorderFolders | folderService.reorderFolders(teamId, req) | 200 |

---

### 4.4 EnvironmentController
- **File:** `controller/EnvironmentController.java`
- **Base Path:** `/api/v1/courier/environments`
- **Dependencies:** EnvironmentService
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | POST | `/` | createEnvironment | environmentService.createEnvironment(teamId, userId, req) | 201 |
  | GET | `/` | getEnvironments | environmentService.getEnvironments(teamId) | 200 |
  | GET | `/active` | getActiveEnvironment | environmentService.getActiveEnvironment(teamId) | 200 |
  | GET | `/{environmentId}` | getEnvironment | environmentService.getEnvironment(environmentId, teamId) | 200 |
  | PUT | `/{environmentId}` | updateEnvironment | environmentService.updateEnvironment(environmentId, teamId, req) | 200 |
  | PUT | `/{environmentId}/activate` | activateEnvironment | environmentService.setActiveEnvironment(environmentId, teamId) | 200 |
  | DELETE | `/{environmentId}` | deleteEnvironment | environmentService.deleteEnvironment(environmentId, teamId) | 204 |
  | POST | `/{environmentId}/clone` | cloneEnvironment | environmentService.cloneEnvironment(environmentId, teamId, userId, req) | 201 |
  | GET | `/{environmentId}/variables` | getEnvironmentVariables | environmentService.getEnvironmentVariables(environmentId, teamId) | 200 |
  | PUT | `/{environmentId}/variables` | saveEnvironmentVariables | environmentService.saveEnvironmentVariables(environmentId, teamId, req) | 200 |

---

### 4.5 VariableController
- **File:** `controller/VariableController.java`
- **Base Path:** `/api/v1/courier/variables/global`
- **Dependencies:** VariableService
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | GET | `/` | getGlobalVariables | variableService.getGlobalVariables(teamId) | 200 |
  | POST | `/` | saveGlobalVariable | variableService.saveGlobalVariable(teamId, req) | 200 |
  | POST | `/batch` | batchSaveGlobalVariables | variableService.batchSaveGlobalVariables(teamId, req) | 200 |
  | DELETE | `/{variableId}` | deleteGlobalVariable | variableService.deleteGlobalVariable(variableId, teamId) | 204 |

---

### 4.6 HistoryController
- **File:** `controller/HistoryController.java`
- **Base Path:** `/api/v1/courier/history`
- **Dependencies:** HistoryService
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | GET | `/` | getHistory | historyService.getHistory(teamId, page, size) | 200 |
  | GET | `/user/{userId}` | getUserHistory | historyService.getUserHistory(teamId, userId, page, size) | 200 |
  | GET | `/method/{method}` | getHistoryByMethod | historyService.getHistoryByMethod(teamId, method, page, size) | 200 |
  | GET | `/search` | searchHistory | historyService.searchHistory(teamId, query) | 200 |
  | GET | `/{historyId}` | getHistoryDetail | historyService.getHistoryDetail(historyId, teamId) | 200 |
  | DELETE | `/{historyId}` | deleteHistory | historyService.deleteHistoryEntry(historyId, teamId) | 204 |
  | DELETE | `/` | clearHistory | historyService.clearOldHistory or clearTeamHistory | 204 |

---

### 4.7 ShareController
- **File:** `controller/ShareController.java`
- **Base Path:** `/api/v1/courier` (no suffix -- endpoints include /collections/ and /shared-with-me)
- **Dependencies:** ShareService
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | POST | `/collections/{collectionId}/shares` | shareCollection | shareService.shareCollection(collectionId, teamId, userId, req) | 201 |
  | GET | `/collections/{collectionId}/shares` | getCollectionShares | shareService.getCollectionShares(collectionId, teamId) | 200 |
  | PUT | `/collections/{collectionId}/shares/{sharedWithUserId}` | updateSharePermission | shareService.updateSharePermission(collectionId, sharedWithUserId, teamId, req) | 200 |
  | DELETE | `/collections/{collectionId}/shares/{sharedWithUserId}` | revokeShare | shareService.revokeShare(collectionId, sharedWithUserId, teamId) | 204 |
  | GET | `/shared-with-me` | getSharedWithMe | shareService.getSharedWithUser(userId) | 200 |

---

### 4.8 ProxyController
- **File:** `controller/ProxyController.java`
- **Base Path:** `/api/v1/courier/proxy`
- **Dependencies:** RequestProxyService
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | POST | `/send` | sendRequest | requestProxyService.executeRequest(req, teamId, userId) | 200 |
  | POST | `/send/{requestId}` | executeStoredRequest | requestProxyService.executeStoredRequest(requestId, teamId, userId, envId) | 200 |

---

### 4.9 GraphQLController
- **File:** `controller/GraphQLController.java`
- **Base Path:** `/api/v1/courier/graphql`
- **Dependencies:** GraphQLService
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | POST | `/execute` | executeQuery | graphQLService.executeQuery(req, teamId, userId) | 200 |
  | POST | `/introspect` | introspect | graphQLService.introspect(req, teamId, userId) | 200 |
  | POST | `/validate` | validateQuery | graphQLService.validateQuery(body.query) | 200 |
  | POST | `/format` | formatQuery | graphQLService.formatQuery(body.query) | 200 |

---

### 4.10 ImportController
- **File:** `controller/ImportController.java`
- **Base Path:** `/api/v1/courier/import`
- **Dependencies:** ImportService
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | POST | `/postman` | importPostman | importService.importCollection(teamId, userId, new ImportCollectionRequest("postman", content)) | 201 |
  | POST | `/openapi` | importOpenApi | importService.importCollection(teamId, userId, new ImportCollectionRequest("openapi", content)) | 201 |
  | POST | `/curl` | importCurl | importService.importCollection(teamId, userId, new ImportCollectionRequest("curl", content)) | 201 |

---

### 4.11 CodeGenerationController
- **File:** `controller/CodeGenerationController.java`
- **Base Path:** `/api/v1/courier/codegen`
- **Dependencies:** CodeGenerationService
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | POST | `/generate` | generate | codeGenerationService.generateCode(req, teamId) | 200 |
  | POST | `/generate/all` | generateAll | codeGenerationService.generateCode for each CodeLanguage | 200 |
  | GET | `/languages` | getLanguages | codeGenerationService.getAvailableLanguages() | 200 |

---

### 4.12 RunnerController
- **File:** `controller/RunnerController.java`
- **Base Path:** `/api/v1/courier/runner`
- **Dependencies:** CollectionRunnerService
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | POST | `/start` | startRun | runnerService.startRun(req, teamId, userId) | 201 |
  | GET | `/results` | getRunResults | runnerService.getRunResultsPaged(teamId, page, size) | 200 |
  | GET | `/results/collection/{collectionId}` | getRunResultsByCollection | runnerService.getRunResults(collectionId, teamId) | 200 |
  | GET | `/results/{runResultId}` | getRunResult | runnerService.getRunResult(runResultId, teamId) | 200 |
  | GET | `/results/{runResultId}/detail` | getRunResultDetail | runnerService.getRunResultDetail(runResultId, teamId) | 200 |
  | POST | `/results/{runResultId}/cancel` | cancelRun | runnerService.cancelRun(runResultId, teamId, userId) | 200 |
  | DELETE | `/results/{runResultId}` | deleteRunResult | runnerService.deleteRunResult(runResultId, teamId) | 204 |

---

### 4.13 HealthController
- **File:** `controller/HealthController.java`
- **Base Path:** `/api/v1/courier`
- **Bean Name:** `"courierHealthController"` (avoids collision with core HealthController)
- **Auth:** NONE (no @PreAuthorize)
- **Endpoints:**
  | HTTP | Path | Method | Service Call | Status |
  |---|---|---|---|---|
  | GET | `/health` | health | Returns Map{status:"UP", service: COURIER_SERVICE_NAME, timestamp} | 200 |

---

## 5. Mappers (13)

All mappers in `com.codeops.courier.dto.mapper`.
Convention: `@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))`.
**Important:** Boolean `is*` fields require explicit `@Mapping(target = "isXxx", source = "xxx")` due to Lombok/JavaBeans property name mismatch.

---

### 5.1 CollectionMapper
- **File:** `dto/mapper/CollectionMapper.java`
- **Methods:**
  - `Collection toEntity(CreateCollectionRequest)` -- ignores id, createdAt, updatedAt, teamId, createdBy, preRequestScript, postResponseScript, folders, variables; sets `shared = "false"`
  - `CollectionResponse toResponse(Collection)` -- `@Mapping(target = "isShared", source = "shared")`, ignores folderCount, requestCount
  - `CollectionSummaryResponse toSummaryResponse(Collection)` -- `@Mapping(target = "isShared", source = "shared")`, ignores folderCount, requestCount

### 5.2 EnvironmentMapper
- **File:** `dto/mapper/EnvironmentMapper.java`
- **Methods:**
  - `Environment toEntity(CreateEnvironmentRequest)` -- ignores id, createdAt, updatedAt, teamId, createdBy, variables; sets `active = "false"`
  - `EnvironmentResponse toResponse(Environment)` -- `@Mapping(target = "isActive", source = "active")`, ignores variableCount

### 5.3 EnvironmentVariableMapper
- **File:** `dto/mapper/EnvironmentVariableMapper.java`
- **Methods:**
  - `EnvironmentVariableResponse toResponse(EnvironmentVariable)` -- `@Mapping(target = "isSecret", source = "secret")`, `@Mapping(target = "isEnabled", source = "enabled")`
  - `List<EnvironmentVariableResponse> toResponseList(List<EnvironmentVariable>)`

### 5.4 FolderMapper
- **File:** `dto/mapper/FolderMapper.java`
- **Methods:**
  - `Folder toEntity(CreateFolderRequest)` -- ignores id, createdAt, updatedAt, collection, parentFolder, subFolders, requests, preRequestScript, postResponseScript, authType, authConfig, sortOrder
  - `FolderResponse toResponse(Folder)` -- `@Mapping(target = "collectionId", source = "collection.id")`, `@Mapping(target = "parentFolderId", source = "parentFolder.id")`, ignores subFolderCount, requestCount

### 5.5 GlobalVariableMapper
- **File:** `dto/mapper/GlobalVariableMapper.java`
- **Methods:**
  - `GlobalVariableResponse toResponse(GlobalVariable)` -- `@Mapping(target = "isSecret", source = "secret")`, `@Mapping(target = "isEnabled", source = "enabled")`
  - `List<GlobalVariableResponse> toResponseList(List<GlobalVariable>)`

### 5.6 RequestAuthMapper
- **File:** `dto/mapper/RequestAuthMapper.java`
- **Methods:**
  - `RequestAuthResponse toResponse(RequestAuth)` -- no custom mappings

### 5.7 RequestBodyMapper
- **File:** `dto/mapper/RequestBodyMapper.java`
- **Methods:**
  - `RequestBodyResponse toResponse(RequestBody)` -- no custom mappings

### 5.8 RequestHeaderMapper
- **File:** `dto/mapper/RequestHeaderMapper.java`
- **Methods:**
  - `RequestHeaderResponse toResponse(RequestHeader)` -- `@Mapping(target = "isEnabled", source = "enabled")`
  - `List<RequestHeaderResponse> toResponseList(List<RequestHeader>)`

### 5.9 RequestHistoryMapper
- **File:** `dto/mapper/RequestHistoryMapper.java`
- **Methods:**
  - `RequestHistoryResponse toResponse(RequestHistory)` -- no custom mappings
  - `RequestHistoryDetailResponse toDetailResponse(RequestHistory)` -- no custom mappings

### 5.10 RequestMapper
- **File:** `dto/mapper/RequestMapper.java`
- **Methods:**
  - `Request toEntity(CreateRequestRequest)` -- ignores id, createdAt, updatedAt, folder, headers, params, body, auth, scripts, sortOrder
  - `RequestResponse toResponse(Request)` -- `@Mapping(target = "folderId", source = "folder.id")`, ignores headers, params, body, auth, scripts (assembled in service)
  - `RequestSummaryResponse toSummaryResponse(Request)` -- no custom mappings

### 5.11 RequestParamMapper
- **File:** `dto/mapper/RequestParamMapper.java`
- **Methods:**
  - `RequestParamResponse toResponse(RequestParam)` -- `@Mapping(target = "isEnabled", source = "enabled")`
  - `List<RequestParamResponse> toResponseList(List<RequestParam>)`

### 5.12 RequestScriptMapper
- **File:** `dto/mapper/RequestScriptMapper.java`
- **Methods:**
  - `RequestScriptResponse toResponse(RequestScript)` -- no custom mappings
  - `List<RequestScriptResponse> toResponseList(List<RequestScript>)`

### 5.13 RunResultMapper
- **File:** `dto/mapper/RunResultMapper.java`
- **Methods:**
  - `RunResultResponse toResponse(RunResult)` -- no custom mappings
  - `RunIterationResponse toIterationResponse(RunIteration)` -- no custom mappings
  - `List<RunIterationResponse> toIterationResponseList(List<RunIteration>)`

---

## 6. Config (1)

### 6.1 HttpClientConfig
- **File:** `config/HttpClientConfig.java`
- **Annotation:** `@Configuration`
- **Bean:** `@Bean public HttpClient courierHttpClient()`
  - `connectTimeout` = `Duration.ofMillis(AppConstants.COURIER_DEFAULT_TIMEOUT_MS)` (30000ms)
  - `followRedirects` = `HttpClient.Redirect.NEVER` (manual redirect tracking in RequestProxyService)
  - `version` = `HttpClient.Version.HTTP_1_1`

### 6.2 AppConstants (Courier-specific)
- `COURIER_API_PREFIX = "/api/v1/courier"`
- `COURIER_SERVICE_NAME = "codeops-courier"`
- `COURIER_RATE_LIMIT_REQUESTS = 100`
- `COURIER_RATE_LIMIT_WINDOW_SECONDS = 60`
- `COURIER_DEFAULT_TIMEOUT_MS = 30000`
- `COURIER_MAX_TIMEOUT_MS = 300000`
- `COURIER_MIN_TIMEOUT_MS = 1000`
- `COURIER_MAX_REDIRECT_COUNT = 10`
- `COURIER_MAX_RESPONSE_BODY_SIZE = 10 * 1024 * 1024` (10 MB)
- `COURIER_HISTORY_BODY_TRUNCATE_SIZE = 1024 * 1024` (1 MB)
- `COURIER_USER_AGENT = "CodeOps-Courier/1.0"`
- `COURIER_SCRIPT_TIMEOUT_SECONDS = 5`
- `COURIER_SCRIPT_MAX_STATEMENTS = 100000`
- `COURIER_SCRIPT_MAX_CONSOLE_LINES = 1000`
- `COURIER_SCRIPT_MAX_OUTPUT_SIZE = 1024 * 1024` (1 MB)

---

## Summary Statistics

| Category | Count |
|---|---|
| Entities | 18 |
| Enums | 7 |
| Repositories | 18 |
| Services | 22 |
| Controllers | 13 |
| Mappers | 13 |
| Config Classes | 1 |
| **Total Java Files** | **72** |
| Controller Endpoints | ~85 |
| Supported Import Formats | 3 (Postman v2.1, OpenAPI 3.x, cURL) |
| Supported Export Formats | 3 (Postman v2.1, OpenAPI 3.0.3, Native) |
| Code Generation Languages | 12 |
| Auth Types | 10 |
| Body Types | 10 |
| HTTP Methods | 7 |
