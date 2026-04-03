FROM confluentinc/cp-kafka:7.5.0

USER root
COPY start.sh /start.sh
RUN chmod +x /start.sh
USER appuser

CMD ["/start.sh"]
