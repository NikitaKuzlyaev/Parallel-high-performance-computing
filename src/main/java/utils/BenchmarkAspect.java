package utils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class BenchmarkAspect {

    private static final Logger logger = LoggerFactory.getLogger(BenchmarkAspect.class);

    @Around("@annotation(utils.Benchmark)")
    public Object measureExecutionTime(ProceedingJoinPoint jp) throws Throwable {

        long enterTime = System.nanoTime();

        try {

            Object res = jp.proceed();
            return res;

        } finally {

            long exitTime = System.nanoTime();
            long duration = exitTime - enterTime;

            logger.info("{} executed in {} ms", jp.getSignature().toShortString(), duration / 1_000_000);
        }

    }

}
