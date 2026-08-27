UPDATE workspace AS w
SET access_mode = 'WRITE_WITH_APPROVAL',
    updated_at = CURRENT_TIMESTAMP
WHERE w.storage_type = 'UNBOUND'
  AND w.access_mode = 'READ_ONLY'
  AND EXISTS (
      SELECT 1
      FROM workspace_binding AS b
      WHERE b.workspace_id = w.id
  );
