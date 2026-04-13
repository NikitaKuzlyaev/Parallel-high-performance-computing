import math
import statistics

# Данные измерений
data = [10349, 10200, 10735, 10547, 8872, 11064, 10887, 10546, 10423, 11204]

mean = statistics.mean(data) # Среднее значение

std_dev = statistics.stdev(data) # Стандартное отклонение

n = len(data) # Размер выборки

# Критическое значение t для 95% (n=10 → ~2.262)
t_critical = 2.262

margin = t_critical * (std_dev / math.sqrt(n)) # Погрешность

lower = mean - margin
upper = mean + margin

print(f"Среднее значение: {mean:.2f}")
print(f"Стандартное отклонение: {std_dev:.2f}")
print(f"95% доверительный интервал: [{lower:.2f}, {upper:.2f}]")

