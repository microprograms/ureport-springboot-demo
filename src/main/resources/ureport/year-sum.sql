select a.year, sum(b.num) sum
from table1 a, table1 b
where a.year >= b.year
group by a.year