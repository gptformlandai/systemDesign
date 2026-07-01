MATCH (a:Account {accountId: 'a2'})-->(signal)<--(other:Account)
WHERE other <> a
RETURN other.accountId AS relatedAccount, labels(signal) AS sharedSignalType, count(*) AS sharedSignals
ORDER BY sharedSignals DESC;

MATCH path = (flagged:Account)-[*1..2]-(candidate:Account)
WHERE flagged.status = 'SUSPENDED'
  AND candidate.status = 'ACTIVE'
RETURN DISTINCT candidate.accountId AS activeAccountNearSuspended, length(path) AS hops
ORDER BY hops;